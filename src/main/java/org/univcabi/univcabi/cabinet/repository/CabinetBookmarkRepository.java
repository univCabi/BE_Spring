package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetBookmark;
import org.univcabi.univcabi.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface CabinetBookmarkRepository extends JpaRepository<CabinetBookmark,Long> {
    boolean existsByUserAndCabinetAndDeletedAtIsNull(User user, Cabinet cabinet);

    Optional<CabinetBookmark> findByUserAndCabinetAndDeletedAtIsNull(User user, Cabinet cabinet);

    // 북마크 목록 조회 시 Cabinet, Building을 함께 초기화하여 N+1(Bookmark -> Cabinet)을 방지한다.
    @Query("SELECT b FROM CabinetBookmark b " +
            "JOIN FETCH b.cabinet c " +
            "JOIN FETCH c.buildingId " +
            "WHERE b.user = :user AND b.deletedAt IS NULL")
    List<CabinetBookmark> findAllByUserAndDeletedAtIsNullFetchCabinetAndBuilding(@Param("user") User user);
}
