package org.univcabi.univcabi.cabinet.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDateTime;

public record CabinetBookmarkListVo(
        Long cabinetId,
        BuildingName building,
        Integer floor,
        String section,
        String cabinetNumber,
        CabinetStatus status,
        LocalDateTime createdAt
) {
}
