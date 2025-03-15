package org.univcabi.univcabi.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthnResponseDto {
    private String message; // 응답 메세지 반환
    private String studentNumber;
}
