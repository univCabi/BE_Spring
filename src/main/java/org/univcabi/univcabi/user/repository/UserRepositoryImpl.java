package org.univcabi.univcabi.user.repository;


import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.univcabi.univcabi.auth.entity.QAuthn;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;
import org.univcabi.univcabi.cabinet.entity.QBuilding;
import org.univcabi.univcabi.cabinet.entity.QCabinet;
import org.univcabi.univcabi.cabinet.entity.QCabinetHistory;
import org.univcabi.univcabi.user.entity.QUser;
import org.univcabi.univcabi.user.entity.User;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<CabinetHistory> getLatestCabinetHistoryByStudentNumber(String studentNumber){
        QCabinetHistory history = QCabinetHistory.cabinetHistory;
        QCabinet cabinet = QCabinet.cabinet;
        QBuilding building = QBuilding.building;
        QUser user =QUser.user;
        QAuthn authn = QAuthn.authn;

        CabinetHistory result = queryFactory
                .selectFrom(history)
                .join(history.user, user).fetchJoin()
                .join(history.cabinet,cabinet).fetchJoin()
                .join(user.authn,authn).fetchJoin()
                .join(cabinet.buildingId,building)
                .where(
                        authn.studentNumber.eq(studentNumber),
                        history.endedAt.isNull()
                )
                .orderBy((history.createdAt.desc()))
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<User> findUserByStudentNumber(String studentNumber){
        QUser user = QUser.user;
        QAuthn authn =QAuthn.authn;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(user)
                        .join(user.authn,authn)
                        .where(authn.studentNumber.eq(studentNumber))
                        .fetchOne()
        );
    }
}
