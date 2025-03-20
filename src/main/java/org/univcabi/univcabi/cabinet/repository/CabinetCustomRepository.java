package org.univcabi.univcabi.cabinet.repository;

import org.univcabi.univcabi.cabinet.entity.Cabinet;

import java.util.Optional;

public interface CabinetCustomRepository {

    Optional<Cabinet> findOneCabinetInfoByCabinetId(Long cabinetId);
}
