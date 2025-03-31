package org.univcabi.univcabi.auth.vo;

import org.univcabi.univcabi.auth.entity.AuthnRole;

public record AuthnLoginVo(
        String studentNumber,
        String password
) {
}
