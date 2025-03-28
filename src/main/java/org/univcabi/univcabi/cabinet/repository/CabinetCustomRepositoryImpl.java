package org.univcabi.univcabi.cabinet.repository;

import com.querydsl.jpa.impl.JPAInsertClause;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.univcabi.univcabi.cabinet.entity.*;
import org.univcabi.univcabi.cabinet.vo.CabinetRentVo;
import org.univcabi.univcabi.cabinet.vo.CabinetReturnVo;
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

        // 엔티티 자체를 조회하면서 필요한 관계를 함께 로드
        Cabinet result = queryFactory
                .selectFrom(cabinet)
                .join(cabinet.buildingId, building).fetchJoin()  // fetchJoin으로 N+1 문제 방지
                .leftJoin(cabinet.userId, user).fetchJoin()  // 사용자가 없을 수도 있으므로 leftJoin
                .where(cabinet.id.eq(cabinetId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    @Transactional
    public Optional<Cabinet> rentCabinetByCabinetId(CabinetRentVo requestVo) {
        QCabinet cabinet = QCabinet.cabinet;
        QCabinetHistory history = QCabinetHistory.cabinetHistory;

        // 1. 학번으로 사용자 찾기
        User user = queryFactory
                .selectFrom(QUser.user)
                .where(QAuthn.authn.studentNumber.eq(requestVo.studentNumber()))
                .fetchOne();

        if (user == null) {
            return Optional.empty(); // 사용자를 찾을 수 없음
        }

        // 2. 업데이트 실행
        long updatedCount = queryFactory.update(cabinet)
                .set(cabinet.userId, user)
                .set(cabinet.updatedAt, LocalDateTime.now())
                .set(cabinet.status, CabinetStatus.USING) // USING → OCCUPIED로 명확하게 수정
                .where(cabinet.id.eq(requestVo.cabinetId())
                        .and(cabinet.status.eq(CabinetStatus.AVAILABLE))) // 사용 가능한 캐비닛만 대여 가능
                .execute();

        // 업데이트된 행이 없으면 (이미 대여 중이거나 존재하지 않는 캐비닛)
        if (updatedCount == 0) {
            return Optional.empty();
        }

        // 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();

        // 3. 업데이트된 캐비닛 조회
        Cabinet rentedCabinet = queryFactory.selectFrom(cabinet)
                .where(cabinet.id.eq(requestVo.cabinetId()))
                .fetchOne();

        if (rentedCabinet == null) {
            return Optional.empty();
        }


        // 4. CabinetHistory 생성 - QueryDSL을 사용한 직접 삽입
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plus(3, ChronoUnit.MONTHS);

        // queryFactory의 EntityManager를 이용해 JPAInsertClause 생성
        JPAInsertClause insertClause = new JPAInsertClause(entityManager, history);

        // columns과 values 지정
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

        // 5. 업데이트된 캐비닛 반환
        return Optional.ofNullable(
                queryFactory.selectFrom(cabinet)
                        .join(cabinet.buildingId).fetchJoin()
                        .leftJoin(cabinet.userId).fetchJoin()
                        .where(cabinet.id.eq(requestVo.cabinetId()))
                        .fetchOne()
        );
    }

    @Override
    @Transactional
    public Optional<Cabinet> returnCabinetByCabinetId(CabinetReturnVo requestVo) {
        QCabinet cabinet = QCabinet.cabinet;
        QCabinetHistory history = QCabinetHistory.cabinetHistory;

        // 1. 학번으로 사용자 찾기
        User user = queryFactory
                .selectFrom(QUser.user)
                .where(QAuthn.authn.studentNumber.eq(requestVo.studentNumber()))
                .fetchOne();

        if (user == null) {
            return Optional.empty(); // 사용자를 찾을 수 없음
        }

        // 2. 현재 캐비닛 정보 조회
        Cabinet currentCabinet = queryFactory
                .selectFrom(cabinet)
                .where(cabinet.id.eq(requestVo.cabinetId()))
                .fetchOne();

        if (currentCabinet == null) {
            return Optional.empty(); // 캐비닛을 찾을 수 없음
        }

        // 3. 해당 캐비닛의 현재 활성화된 기록 조회
        CabinetHistory activeHistory = queryFactory
                .selectFrom(history)
                .where(history.cabinet.id.eq(requestVo.cabinetId())
                        .and(history.user.id.eq(user.getId()))
                        .and(history.endedAt.isNull()))
                .fetchOne();

        if (activeHistory == null) {
            return Optional.empty(); // 활성화된 대여 기록이 없음
        }

        // 4. 업데이트 실행 - 캐비닛 상태 변경
        long updatedCount = queryFactory.update(cabinet)
                .setNull(cabinet.userId)
                .set(cabinet.updatedAt, LocalDateTime.now())
                .set(cabinet.status, CabinetStatus.AVAILABLE)
                .where(cabinet.id.eq(requestVo.cabinetId())
                        .and(cabinet.status.eq(CabinetStatus.USING))
                        .and(cabinet.userId.id.eq(user.getId())))
                .execute();

        if (updatedCount == 0) {
            return Optional.empty(); // 업데이트 실패
        }

        // 5. 히스토리 레코드 업데이트 - endedAt 설정
        LocalDateTime now = LocalDateTime.now();
        long historyUpdateCount = queryFactory.update(history)
                .set(history.endedAt, now)
                .set(history.updatedAt, now)
                .where(history.id.eq(activeHistory.getId()))
                .execute();

        if (historyUpdateCount == 0) {
            return Optional.empty(); // 업데이트 실패
        }

        // 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();

        // 6. 업데이트된 캐비닛 조회하여 반환
        return Optional.ofNullable(
                queryFactory.selectFrom(cabinet)
                        .join(cabinet.buildingId).fetchJoin()
                        .leftJoin(cabinet.userId).fetchJoin()
                        .where(cabinet.id.eq(requestVo.cabinetId()))
                        .fetchOne()
        );
    }

    @Override
    public Page<CabinetHistory> findCabinetHistoriesByStudentNumber(String studentNumber, Pageable pageable) {
        QCabinetHistory cabinetHistory = QCabinetHistory.cabinetHistory;
        QCabinet cabinet = QCabinet.cabinet;
        QBuilding building = QBuilding.building;
        QUser user = QUser.user;
        QAuthn authn = QAuthn.authn;

        // First execute query to get total count
        long total = Optional.ofNullable(
                queryFactory
                        .select(cabinetHistory.count())
                        .from(cabinetHistory)
                        .join(cabinetHistory.user, user)
                        .where(authn.studentNumber.eq(studentNumber))
                        .fetchOne()
        ).orElse(0L);

        // Then get the paginated results
        List<CabinetHistory> content = queryFactory
                .selectFrom(cabinetHistory)
                .join(cabinetHistory.cabinet, cabinet).fetchJoin()
                .join(cabinet.buildingId, building).fetchJoin()
                .join(cabinetHistory.user, user)
                .where(authn.studentNumber.eq(studentNumber))
                .orderBy(cabinetHistory.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }
}
