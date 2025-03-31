package org.univcabi.univcabi.auth.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthnLoginResponseDto {
    private String accessToken;
}
