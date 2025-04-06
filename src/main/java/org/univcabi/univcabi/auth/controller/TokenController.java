package org.univcabi.univcabi.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.auth.dto.response.AuthnLoginResponseDto;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.AuthnService;
import org.univcabi.univcabi.auth.service.TokenService;
import org.univcabi.univcabi.exception.MiddlewareException;

import java.io.IOException;

import static org.univcabi.univcabi.exception.ExceptionStatus.AUTH_MISMATCH_REFRESH_TOKEN;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final AuthnService authnService;
    private final JwtTokenProvider jwtTokenProvider;


    // RefreshToken 관련 예외 책임은 전부 여기로
    @PostMapping("/authn/token/access")
    public ResponseEntity<AuthnLoginResponseDto> refreshAccessToken(@CookieValue(value = "refreshToken")String refreshToken,
                                                                    HttpServletResponse response) throws IOException {

        // 토큰 만료 확인 ( json 형태의 응답이 필요해서 Servlet 사용)
        if(!jwtTokenProvider.validateToken(refreshToken)){
            log.warn("RefreshToken 만료");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"messages\": [{\"token_class\": \"RefreshToken\"}]}");
            return null;
        }

        String studentNumber = jwtTokenProvider.getStudentNumberFromToken(refreshToken);

        // redis의 refreshToken 과 요청의 refreshToken 일치 여부 확인
        String storedToken = tokenService.getRefreshToken(studentNumber);
        if(storedToken ==null|| !storedToken.equals(refreshToken)){
            throw new MiddlewareException(AUTH_MISMATCH_REFRESH_TOKEN);
        }

        // 새로운 accessToken 발급
        AuthnRole role = authnService.getUserRole(studentNumber);
        String newAccessToken = jwtTokenProvider.generateAccessToken(studentNumber,role);

        log.info("RefreshToken 인증 성공, 새로운 AccessToken 발급 완료");

        return ResponseEntity.ok(AuthnLoginResponseDto.builder()
                .accessToken(newAccessToken)
                .build());
    }

}
