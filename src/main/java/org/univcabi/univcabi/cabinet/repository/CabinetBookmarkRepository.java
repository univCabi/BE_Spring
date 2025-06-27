package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetBookmark;
import org.univcabi.univcabi.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface CabinetBookmarkRepository extends JpaRepository<CabinetBookmark,Long> {
    boolean existsByUserAndCabinetAndDeletedAtIsNull(User user, Cabinet cabinet);

    Optional<CabinetBookmark> findByUserAndCabinetAndDeletedAtIsNull(User user, Cabinet cabinet);

    List<CabinetBookmark> findAllByUserAndDeletedAtIsNull(User user);
}
