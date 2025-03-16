package org.univcabi.univcabi.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    // 쿠키에서 refresh 토큰 가져오기
    public String getRefreshTokenFromCookie(HttpServletRequest request){
        if(request.getCookies() != null){
            for(Cookie cookie : request.getCookies()){
                if(cookie.getName().equals("refresh_token")){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // 쿠키 저장
    private void setCookie(HttpServletResponse response,String accessToken, String name){
        Cookie cookie = new Cookie(name,accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    // 쿠키 삭제
    private void clearCookie(HttpServletResponse response,String name){
        Cookie cookie = new Cookie(name,null);
        cookie.setMaxAge(0); // 쿠키 만료 -> 브라우저는 만료된 쿠키 삭제
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public void setAccessTokenToCookie(HttpServletResponse response,String accessToken){
        setCookie(response,accessToken,"access_token");
    }

    public void setRefreshTokenToCookie(HttpServletResponse response,String refreshToken){
        setCookie(response,refreshToken,"refresh_token");
    }

    public void clearAccessTokenToCookie(HttpServletResponse response){
        clearCookie(response,"access_token");
    }

    public void clearRefreshTokenToCookie(HttpServletResponse response){
        clearCookie(response,"refresh_token");
    }
}
