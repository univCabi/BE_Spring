package org.univcabi.univcabi.auth.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthnLoginResponseDto {
    private String message; // 응답 메세지 반환
    private String accessToken;

    public static AuthnLoginResponseDto of(String accessToken) {
        return new AuthnLoginResponseDto("로그인 성공",accessToken);
    }
}
