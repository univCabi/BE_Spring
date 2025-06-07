package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.BuildingName;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    Building findBuildingById(Long id);

    Optional<Building> findBuildingByNameAndFloorAndSection(BuildingName name, Integer floor, String section);
}
