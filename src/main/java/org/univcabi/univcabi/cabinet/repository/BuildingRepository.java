package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.univcabi.cabinet.entity.Building;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    Building findBuildingById(Long id);
}
