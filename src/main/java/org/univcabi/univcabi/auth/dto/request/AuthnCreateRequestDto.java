package org.univcabi.univcabi.auth.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.auth.entity.AuthnRole;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthnCreateRequestDto {
    private String studentNumber;
    private String password;
    private AuthnRole role = AuthnRole.NORMAL;
}
