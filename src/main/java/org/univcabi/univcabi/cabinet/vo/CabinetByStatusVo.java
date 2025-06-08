package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetPosition;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.user.entity.User;

import java.time.LocalDate;

// CabinetPosition, User 엔터티를 받은 후 dto로 변환
public record CabinetByStatusVo(
        Long id,
        BuildingName building,
        Integer floor,
        String section,
        CabinetPosition position,
        String cabinetNumber,
        CabinetStatus status,
        User user,
        String reason,
        LocalDate rentalStartDate,
        LocalDate overDate,
        LocalDate brokenDate
) {
}
