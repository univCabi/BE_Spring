package org.univcabi.univcabi.user.repository;


import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.univcabi.univcabi.auth.entity.QAuthn;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;
import org.univcabi.univcabi.cabinet.entity.QBuilding;
import org.univcabi.univcabi.cabinet.entity.QCabinet;
import org.univcabi.univcabi.cabinet.entity.QCabinetHistory;
import org.univcabi.univcabi.exception.ControllerException;
import org.univcabi.univcabi.exception.RepositoryException;
import org.univcabi.univcabi.user.entity.QUser;
import org.univcabi.univcabi.user.entity.User;

import java.util.Optional;

import static org.univcabi.univcabi.exception.ExceptionStatus.*;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    // StudentNumber에 해당 하는 User의 building, cabinet, cabinet_history 정보 조회
    @Override
    public Optional<CabinetHistory> getLatestCabinetHistoryByStudentNumber(String studentNumber){

        if(studentNumber ==null) {
            throw new ControllerException(USER_INVALID_STUDENT_NUMBER);
        }

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
                        history.endedAt.isNull()  // endDate 가 Null인 경우에만 조회( 반납 했을시 Null 반환하도록 )
                )
                .orderBy((history.createdAt.desc()))
                .limit(1)  // 1개
                .fetchOne();

        // result 값이 Null인 경우도 정상임 -> 빌리고 있는 사물함이 없을 수 있음.
        return Optional.ofNullable(result);
    }


    // User의 정보 조회
    @Override
    public Optional<User> findUserByStudentNumber(String studentNumber){

        if(studentNumber ==null) {
            throw new ControllerException(USER_INVALID_STUDENT_NUMBER);
        }

        QUser user = QUser.user;
        QAuthn authn =QAuthn.authn;

        User result = queryFactory
                .selectFrom(user)
                .join(user.authn,authn)
                .where(authn.studentNumber.eq(studentNumber))
                .fetchOne();

        if (result == null){
            throw new RepositoryException(USER_NOT_FOUND);
        }

        return Optional.of(result);
    }

    @Override
    public long updateUserVisibilityByStudentNumber(String studentNumber, Boolean isVisible){

        if(studentNumber == null) {
            throw new ControllerException(USER_INVALID_STUDENT_NUMBER);
        }else if(isVisible == null){
            throw new ControllerException(USER_INVALID_VISIBILITY);
        }

        QUser user = QUser.user;
        QAuthn authn =QAuthn.authn;

        return queryFactory
                .update(user)
                .set(user.isVisible,isVisible)
                .where(user.authn.id.eq(
                        JPAExpressions
                                .select(authn.id)
                                .from(authn)
                                .where(authn.studentNumber.eq(studentNumber))
                ))
                .execute();
    }

}
