package org.univcabi.univcabi.user.vo;

import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.cabinet.entity.BuildingName;

public record AdminUserCreateVo(
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
