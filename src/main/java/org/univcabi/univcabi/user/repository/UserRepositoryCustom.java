package org.univcabi.univcabi.user.repository;

import org.univcabi.univcabi.cabinet.entity.CabinetHistory;
import org.univcabi.univcabi.user.entity.User;

import java.util.Optional;

public interface UserRepositoryCustom {

    Optional<CabinetHistory> getLatestCabinetHistoryByStudentNumber(String studentNumber);

    Optional<User> findUserByStudentNumber(String studentNumber);
}
