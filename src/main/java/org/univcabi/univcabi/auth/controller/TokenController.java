package org.univcabi.univcabi.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.auth.dto.AuthnResponseDto;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.TokenService;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/authn/token/access")
    public ResponseEntity<AuthnResponseDto> refreshAccessToken(HttpServletRequest request, HttpServletResponse response){
        String refreshToken = tokenService.getRefreshTokenFromCookie(request);

        if(refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)){
            return  ResponseEntity.status(401).body(AuthnResponseDto.builder().message("유효하지 않은 RefreshToken").build());
        }

        String studentNumber = jwtTokenProvider.getStudentNumberFromToken(refreshToken);
        String storedToken = tokenService.getRefreshToken(studentNumber);

        if(storedToken == null || !storedToken.equals(refreshToken))
        {
            return ResponseEntity.status(401).body(AuthnResponseDto.builder().message("RefreshToken 불일치").build());
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(studentNumber,"USER");

        return ResponseEntity.ok(AuthnResponseDto.builder()
                        .studentNumber(studentNumber).message("새로운 AccessToken 발급").accessToken(newAccessToken).build());
    }

}
