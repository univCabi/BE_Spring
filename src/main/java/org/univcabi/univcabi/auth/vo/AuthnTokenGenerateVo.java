package org.univcabi.univcabi.auth.vo;

import org.univcabi.univcabi.auth.entity.AuthnRole;

// login 시에 해당 USER의 권한을 판단하는 Vo
public record AuthnTokenGenerateVo(
        String studentNumber,
        AuthnRole role
) {
}
