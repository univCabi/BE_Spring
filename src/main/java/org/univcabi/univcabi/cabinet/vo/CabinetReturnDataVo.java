package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

// CabinetReturnResultVo에 활용되는 Vo
public record CabinetReturnDataVo(
        Long id,
        BuildingName buildingName,
        String cabinetNumber,
        CabinetStatus status,
        String name
) {
}
