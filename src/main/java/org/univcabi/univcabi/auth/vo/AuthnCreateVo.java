package org.univcabi.univcabi.auth.vo;

import org.univcabi.univcabi.auth.entity.AuthnRole;

public record AuthnCreateVo(
        String studentNumber,
        String password,
        AuthnRole role // NORMAL 고정값
) {
}
