package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;

import java.util.List;

public interface CabinetHistoryRepository extends JpaRepository<CabinetHistory, Long> {
    List<CabinetHistory> findByCabinetId(Long cabinetId);
}