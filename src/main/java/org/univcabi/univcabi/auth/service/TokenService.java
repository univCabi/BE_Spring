package org.univcabi.univcabi.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-expiration}") private long refreshTokenExpiration;


    // refresh 토큰 저장
    public void storeRefreshToken(String studentNumber, String refreshToken){
        redisTemplate.opsForValue().set("refresh:"+studentNumber,refreshToken,refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    // refresh 토큰 조회
    public String getRefreshToken(String studentNumber){
        return redisTemplate.opsForValue().get("refresh:"+studentNumber);
    }

    // refresh 토큰 삭제
    public void deleteRefreshToken(String studentNumber){
        redisTemplate.delete("refresh:"+studentNumber); // refresh:학생번호 형식의 데이터 삭제
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken){
        return ResponseCookie.from("refreshToken",refreshToken)
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();
    }

    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("refreshToken")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
