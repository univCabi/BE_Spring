package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

public record CabinetDataVo(
        Long id,
        Integer cabinetNumber,
        Integer cabinetXPos,
        Integer cabinetYPos,
        CabinetStatus status,
        Boolean isVisible,
        String username,
        Boolean isMine,
        Boolean isRentAvailable,
        Boolean isFree
) {
}