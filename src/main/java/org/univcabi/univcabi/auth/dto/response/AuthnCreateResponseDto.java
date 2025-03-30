package org.univcabi.univcabi.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthnCreateResponseDto {
    private String studentNumber;
    private String message;
}
