package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDateTime;

public record CabinetDetailVo(
        Integer floor,
        String section,
        BuildingName building,
        String cabinetNumber,
        CabinetStatus status,
        Boolean isVisible,
        String username,
        Boolean isMine,
        LocalDateTime expiredAt
)
{}