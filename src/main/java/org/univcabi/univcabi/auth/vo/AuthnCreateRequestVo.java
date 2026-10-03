package org.univcabi.univcabi.auth.vo;

import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.cabinet.entity.BuildingName;

public record AuthnCreateRequestVo(
        String name,
        String affiliation,
        String phoneNumber,
        String studentNumber,
        String password,
        AuthnRole role,
        BuildingName buildingName,
        Integer floor,
        String section
) {
}
