package org.univcabi.univcabi.cabinet.repository;

import com.querydsl.jpa.impl.JPAInsertClause;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.cabinet.entity.*;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.RepositoryException;
import org.univcabi.univcabi.user.entity.QUser;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.auth.entity.QAuthn;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Repository
public class CabinetCustomRepositoryImpl implements CabinetCustomRepository {

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    // 명시적 생성자 주입
    public CabinetCustomRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<Cabinet> findOneCabinetInfoByCabinetId(Long cabinetId) {
        QCabinet cabinet = QCabinet.cabinet;
        QBuilding building = QBuilding.building;
        QUser user = QUser.user;

        // null 체크 추가
        if (cabinetId == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_INVALID_ID);
        }

        // 엔티티 자체를 조회하면서 필요한 관계를 함께 로드
        Cabinet result = queryFactory
                .selectFrom(cabinet)
                .join(cabinet.buildingId, building).fetchJoin()  // fetchJoin으로 N+1 문제 방지
                .leftJoin(cabinet.userId, user).fetchJoin()  // 사용자가 없을 수도 있으므로 leftJoin
                .where(cabinet.id.eq(cabinetId))
                .fetchOne();

        if (result == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_NOT_FOUND);
        }

        return Optional.ofNullable(result);
    }

    @Override
    @Transactional
    public Optional<Cabinet> rentCabinetByCabinetId(Long cabinetId, String studentNumber) {
        QCabinet cabinet = QCabinet.cabinet;
        QCabinetHistory history = QCabinetHistory.cabinetHistory;

        // null 체크 추가
        if (cabinetId == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_INVALID_ID);
        } else if (studentNumber == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_INVALID_STUDENT_NUMBER);
        }

        // 1. 학번으로 사용자 찾기
        User user = queryFactory
                .selectFrom(QUser.user)
                .join(QAuthn.authn).on(QAuthn.authn.user.eq(QUser.user))
                .where(QAuthn.authn.studentNumber.eq(studentNumber))
                .fetchOne();

        if (user == null) {
            throw new RepositoryException(ExceptionStatus.USER_NOT_FOUND);
        }

        // 캐비닛 존재 여부 확인
        Cabinet existingCabinet = queryFactory.selectFrom(cabinet)
                .where(cabinet.id.eq(cabinetId))
                .fetchOne();

        if (existingCabinet == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_NOT_FOUND);
        }

        // 캐비닛이 이미 사용 중인지 확인
        if (existingCabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new RepositoryException(ExceptionStatus.CABINET_ALREADY_USING);
        }

        // 2. 업데이트 실행
        long updatedCount = queryFactory.update(cabinet)
                .set(cabinet.userId, user)
                .set(cabinet.updatedAt, LocalDateTime.now())
                .set(cabinet.status, CabinetStatus.USING)
                .where(cabinet.id.eq(cabinetId)
                        .and(cabinet.status.eq(CabinetStatus.AVAILABLE)))
                .execute();

        // 업데이트된 행이 없으면 (동시성 문제 등으로 인한 실패)
        if (updatedCount == 0) {
            throw new RepositoryException(ExceptionStatus.CABINET_RENT_FAILED);
        }

        // 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();

        // 3. 업데이트된 캐비닛 조회
        Cabinet rentedCabinet = queryFactory.selectFrom(cabinet)
                .where(cabinet.id.eq(cabinetId))
                .fetchOne();

        if (rentedCabinet == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_RENT_FAILED);
        }

        // 4. CabinetHistory 생성
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiredAt = now.plus(3, ChronoUnit.MONTHS);

            JPAInsertClause insertClause = new JPAInsertClause(entityManager, history);
            insertClause.columns(
                    history.user,
                    history.cabinet,
                    history.createdAt,
                    history.updatedAt,
                    history.expiredAt
            ).values(
                    user,
                    rentedCabinet,
                    now,
                    now,
                    expiredAt
            ).execute();

            // 변경 사항 적용
            entityManager.flush();
        } catch (Exception e) {
            throw new RepositoryException(ExceptionStatus.CABINET_HISTORY_CREATION_FAILED);
        }

        // 5. 업데이트된 캐비닛 반환
        Cabinet result = queryFactory.selectFrom(cabinet)
                .join(cabinet.buildingId).fetchJoin()
                .leftJoin(cabinet.userId).fetchJoin()
                .where(cabinet.id.eq(cabinetId))
                .fetchOne();

        if (result == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_RENT_FAILED);
        }

        return Optional.of(result);
    }

    @Override
    @Transactional
    public Optional<Cabinet> returnCabinetByCabinetId(Long cabinetId, String studentNumber) {
        QCabinet cabinet = QCabinet.cabinet;
        QCabinetHistory history = QCabinetHistory.cabinetHistory;

        // null 체크 추가
        if (cabinetId == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_INVALID_ID);
        } else if (studentNumber == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_INVALID_STUDENT_NUMBER);
        }

        // 1. 학번으로 사용자 찾기
        User user = queryFactory
                .selectFrom(QUser.user)
                .join(QAuthn.authn).on(QAuthn.authn.user.eq(QUser.user))
                .where(QAuthn.authn.studentNumber.eq(studentNumber))
                .fetchOne();

        if (user == null) {
            throw new RepositoryException(ExceptionStatus.USER_NOT_FOUND);
        }

        // 2. 현재 캐비닛 정보 조회
        Cabinet currentCabinet = queryFactory
                .selectFrom(cabinet)
                .where(cabinet.id.eq(cabinetId))
                .fetchOne();

        if (currentCabinet == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_NOT_FOUND);
        }

        // 캐비닛이 현재 사용 중인지 확인
        if (currentCabinet.getStatus() != CabinetStatus.USING) {
            throw new RepositoryException(ExceptionStatus.CABINET_NOT_USING);
        }

        // 현재 사용자가 대여자인지 확인
        if (currentCabinet.getUserId() == null || !currentCabinet.getUserId().getId().equals(user.getId())) {
            throw new RepositoryException(ExceptionStatus.CABINET_INVALID_STUDENT_NUMBER);
        }

        // 3. 해당 캐비닛의 현재 활성화된 기록 조회
        CabinetHistory activeHistory = queryFactory
                .selectFrom(history)
                .where(history.cabinet.id.eq(cabinetId)
                        .and(history.user.id.eq(user.getId()))
                        .and(history.endedAt.isNull()))
                .fetchOne();

        if (activeHistory == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_HISTORY_NOT_FOUND);
        }

        // 4. 업데이트 실행 - 캐비닛 상태 변경
        long updatedCount = queryFactory.update(cabinet)
                .setNull(cabinet.userId)
                .set(cabinet.updatedAt, LocalDateTime.now())
                .set(cabinet.status, CabinetStatus.AVAILABLE)
                .where(cabinet.id.eq(cabinetId)
                        .and(cabinet.status.eq(CabinetStatus.USING))
                        .and(cabinet.userId.id.eq(user.getId())))
                .execute();

        if (updatedCount == 0) {
            throw new RepositoryException(ExceptionStatus.CABINET_RETURN_FAILED);
        }

        // 5. 히스토리 레코드 업데이트 - endedAt 설정
        try {
            LocalDateTime now = LocalDateTime.now();
            long historyUpdateCount = queryFactory.update(history)
                    .set(history.endedAt, now)
                    .set(history.updatedAt, now)
                    .where(history.id.eq(activeHistory.getId()))
                    .execute();

            if (historyUpdateCount == 0) {
                throw new RepositoryException(ExceptionStatus.CABINET_HISTORY_UPDATE_FAILED);
            }
        } catch (Exception e) {
            throw new RepositoryException(ExceptionStatus.CABINET_HISTORY_UPDATE_FAILED);
        }

        // 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();

        // 6. 업데이트된 캐비닛 조회하여 반환
        Cabinet result = queryFactory.selectFrom(cabinet)
                .join(cabinet.buildingId).fetchJoin()
                .leftJoin(cabinet.userId).fetchJoin()
                .where(cabinet.id.eq(cabinetId))
                .fetchOne();

        if (result == null) {
            throw new RepositoryException(ExceptionStatus.CABINET_RETURN_FAILED);
        }

        return Optional.of(result);
    }

    @Override
    public Page<CabinetHistory> findCabinetHistoriesByStudentNumber(String studentNumber, Pageable pageable) {
        QCabinetHistory cabinetHistory = QCabinetHistory.cabinetHistory;
        QCabinet cabinet = QCabinet.cabinet;
        QBuilding building = QBuilding.building;
        QUser user = QUser.user;
        QAuthn authn = QAuthn.authn;

        // null 체크 추가
        if (studentNumber == null || studentNumber.isBlank()) {
            throw new RepositoryException(ExceptionStatus.CABINET_INVALID_STUDENT_NUMBER);
        }

        // 사용자 존재 여부 확인
        Long userCount = queryFactory
                .select(user.count())
                .from(user)
                .join(QAuthn.authn).on(QAuthn.authn.user.eq(user))
                .where(QAuthn.authn.studentNumber.eq(studentNumber))
                .fetchOne();

        if (userCount == null || userCount == 0) {
            throw new RepositoryException(ExceptionStatus.USER_NOT_FOUND);
        }

        try {
            // First execute query to get total count
            long total = Optional.ofNullable(
                    queryFactory
                            .select(cabinetHistory.count())
                            .from(cabinetHistory)
                            .join(cabinetHistory.user, user)
                            .join(user.authn, authn)
                            .where(authn.studentNumber.eq(studentNumber))
                            .fetchOne()
            ).orElse(0L);

            // Then get the paginated results
            List<CabinetHistory> content = queryFactory
                    .selectFrom(cabinetHistory)
                    .join(cabinetHistory.cabinet, cabinet).fetchJoin()
                    .join(cabinet.buildingId, building).fetchJoin()
                    .join(cabinetHistory.user, user)
                    .join(user.authn, authn)
                    .where(authn.studentNumber.eq(studentNumber))
                    .orderBy(cabinetHistory.createdAt.desc())
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();

            return new PageImpl<>(content, pageable, total);
        } catch (Exception e) {
            throw new RepositoryException(ExceptionStatus.CABINET_HISTORY_SEARCH_FAILED);
        }
    }
}
