package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;

import java.util.List;
import java.util.Optional;

public interface CabinetHistoryRepository extends JpaRepository<CabinetHistory, Long> {
    List<CabinetHistory> findByCabinetId(Long cabinetId);

    // 가장 최근 사물함 내역 조회
    @Query("SELECT ch FROM CabinetHistory ch WHERE ch.cabinet.id = :cabinetId ORDER BY ch.createdAt DESC")
    Optional<CabinetHistory> findTop1ByCabinetIdOrderByCreatedAtDesc(Long cabinetId);
}