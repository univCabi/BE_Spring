package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.BuildingName;

import java.time.LocalDateTime;

public record CabinetHistoryResponseVo(
    BuildingName building,
    Integer floor,
    String section,
    String cabinetNumber,
    LocalDateTime startDate,
    LocalDateTime endDate
) {
}
