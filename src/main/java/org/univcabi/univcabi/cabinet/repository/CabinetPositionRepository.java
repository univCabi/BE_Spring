package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetPosition;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface CabinetPositionRepository extends JpaRepository<CabinetPosition, Long> {

    Optional<CabinetPosition> findByCabinetId(Cabinet cabinet);

    List<CabinetPosition> findByCabinetIdIn(Collection<Cabinet> cabinetIds);
}
