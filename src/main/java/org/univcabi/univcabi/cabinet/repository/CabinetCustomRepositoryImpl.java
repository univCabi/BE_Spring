package org.univcabi.univcabi.cabinet.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.QBuilding;
import org.univcabi.univcabi.cabinet.entity.QCabinet;
import org.univcabi.univcabi.user.entity.QUser;

import java.util.Optional;

@Repository
public class CabinetCustomRepositoryImpl implements CabinetCustomRepository {

    private JPAQueryFactory queryFactory;

    @Override
    public Optional<Cabinet> findOneCabinetInfoByCabinetId(Long cabinetId) {
        QCabinet cabinet = QCabinet.cabinet;
        QBuilding building = QBuilding.building;
        QUser user = QUser.user;

        // 엔티티 자체를 조회
        Cabinet result = queryFactory
                .selectFrom(cabinet)
                .join(cabinet.buildingId, building).fetchJoin()  // fetchJoin을 사용하여 N+1 문제 방지
                .join(cabinet.userId, user).fetchJoin()
                .where(cabinet.id.eq(cabinetId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
