package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.BuildingName;

public record CabinetStatusCountVo(
        BuildingName name,
        long total,
        long using,
        long overdue,
        long broken,
        long available
) {
}
