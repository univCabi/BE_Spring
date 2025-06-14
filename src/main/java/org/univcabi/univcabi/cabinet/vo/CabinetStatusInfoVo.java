package org.univcabi.univcabi.cabinet.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDate;

// cabinet/admin/change/status api 에 사물함 정보를 반환할 때 사용되는 vo
public record CabinetStatusInfoVo(
        Long id,
        BuildingName buildingName,
        Integer floor,
        String cabinetNumber,
        CabinetStatus status,
        String reason,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
        LocalDate brokenDate,
        String userName
) {
}
