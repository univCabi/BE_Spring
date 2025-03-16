package org.univcabi.univcabi.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.TokenService;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/authn/token/access")
    public ResponseEntity<String> refreshAccessToken(HttpServletRequest request, HttpServletResponse response){
        String refreshToken = tokenService.getRefreshTokenFromCookie(request);

        if(refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)){
            return  ResponseEntity.status(401).body("유효하지 않은 Refresh Token");
        }

        String studentNumber = jwtTokenProvider.getStudentNumberFromToken(refreshToken);
        String storedToken = tokenService.getRefreshToken(studentNumber);

        String newAccessToken = jwtTokenProvider.generateAccessToken(studentNumber,"USER");
        tokenService.setAccessTokenToCookie(response,newAccessToken);

        return ResponseEntity.ok("새로운 Access Token 발급 완료");
    }

}
