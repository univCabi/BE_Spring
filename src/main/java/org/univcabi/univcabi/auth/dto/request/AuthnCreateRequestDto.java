package org.univcabi.univcabi.auth.dto.request;


import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class AuthnCreateRequestDto {
    private String studentNumber;
    private String password;
}
