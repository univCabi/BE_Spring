package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.BuildingName;

public record CabinetLocationVo(
        BuildingName building,
        Integer floors,
        String studentNumber
) {

}
