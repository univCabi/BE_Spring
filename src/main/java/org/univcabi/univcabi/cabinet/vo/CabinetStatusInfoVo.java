package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDate;

// cabinet/admin/change/status api 에 사물함 정보를 반환할 때 사용되는 vo
public record CabinetStatusInfoVo(
        Long id,
        BuildingName buildingName,
        Long floor,
        Long cabinetNumber,
        CabinetStatus status,
        String reason,
        LocalDate brokenDate,
        String userName
) {
}
