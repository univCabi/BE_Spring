package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetPosition;

import java.util.Optional;

public interface CabinetPositionRepository extends JpaRepository<CabinetPosition, Long> {

    Optional<CabinetPosition> findByCabinetId(Cabinet cabinet);
}
