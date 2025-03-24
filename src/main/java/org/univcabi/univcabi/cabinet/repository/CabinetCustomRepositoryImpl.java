package org.univcabi.univcabi.cabinet.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.univcabi.univcabi.cabinet.entity.*;
import org.univcabi.univcabi.cabinet.vo.CabinetHistoryVo;
import org.univcabi.univcabi.cabinet.vo.CabinetRentVo;
import org.univcabi.univcabi.cabinet.vo.CabinetReturnVo;
import org.univcabi.univcabi.user.entity.QUser;
import org.univcabi.univcabi.user.entity.User;
import org.univcabi.univcabi.auth.entity.QAuthn;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CabinetCustomRepositoryImpl implements CabinetCustomRepository {

    private JPAQueryFactory queryFactory;

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
    public Optional<Cabinet> rentCabinetByCabinetId(CabinetRentVo requestVo) {
        QCabinet cabinet = QCabinet.cabinet;

        // 1. 학번으로 사용자 찾기
        User user = queryFactory
                .selectFrom(QUser.user)
                .where(QAuthn.authn.studentNumber.eq(requestVo.studentNumber()))
                .fetchOne();

        if (user == null) {
            return Optional.empty(); // 사용자를 찾을 수 없음
        }

        // 업데이트 실행
        long updatedCount = queryFactory.update(cabinet)
                .set(cabinet.userId, user)
                .set(cabinet.updatedAt, LocalDateTime.now())
                .set(cabinet.status, CabinetStatus.USING) // 상태를 점유됨으로 변경
                .where(cabinet.id.eq(requestVo.cabinetId())
                        .and(cabinet.status.eq(CabinetStatus.AVAILABLE))) // 사용 가능한 캐비닛만 대여 가능
                .execute();

        // 업데이트된 행이 없으면 (이미 대여 중이거나 존재하지 않는 캐비닛)
        if (updatedCount == 0) {
            return Optional.empty();
        }

        // 3. 업데이트된 캐비닛 조회하여 반환
        return Optional.ofNullable(
                queryFactory.selectFrom(cabinet)
                        .join(cabinet.buildingId).fetchJoin()
                        .join(cabinet.userId).fetchJoin()
                        .where(cabinet.id.eq(requestVo.cabinetId()))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Cabinet> returnCabinetByCabinetId(CabinetReturnVo requestVo) {
        QCabinet cabinet = QCabinet.cabinet;

        // 1. 학번으로 사용자 찾기
        User user = queryFactory
                .selectFrom(QUser.user)
                .where(QAuthn.authn.studentNumber.eq(requestVo.studentNumber()))
                .fetchOne();

        if (user == null) {
            return Optional.empty(); // 사용자를 찾을 수 없음
        }

        // 업데이트 실행
        long updatedCount = queryFactory.update(cabinet)
                .set(cabinet.userId, user)
                .set(cabinet.updatedAt, LocalDateTime.now())
                .set(cabinet.status, CabinetStatus.AVAILABLE) // 상태를 점유됨으로 변경
                .where(cabinet.id.eq(requestVo.cabinetId())
                        .and(cabinet.status.eq(CabinetStatus.USING))) // 사용 가능한 캐비닛만 대여 가능
                .execute();

        // 업데이트된 행이 없으면 (이미 대여 중이거나 존재하지 않는 캐비닛)
        if (updatedCount == 0) {
            return Optional.empty();
        }

        // 3. 업데이트된 캐비닛 조회하여 반환
        return Optional.ofNullable(
                queryFactory.selectFrom(cabinet)
                        .join(cabinet.buildingId).fetchJoin()
                        .join(cabinet.userId).fetchJoin()
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
        long total = queryFactory
                .select(cabinetHistory.count())
                .from(cabinetHistory)
                .join(cabinetHistory.user, user)
                .where(authn.studentNumber.eq(studentNumber))
                .fetchOne();

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
