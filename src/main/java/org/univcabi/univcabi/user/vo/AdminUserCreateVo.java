package org.univcabi.univcabi.user.vo;

import org.univcabi.univcabi.auth.entity.AuthnRole;

public record AdminUserCreateVo(
        String name,
        String affiliation,
        String phoneNumber,
        String studentNumber,
        String password,
        AuthnRole role,
        String buildingName,
        Integer floor,
        String section
) {
}
