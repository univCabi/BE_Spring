package org.univcabi.univcabi.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.auth.dto.response.AuthnLoginResponseDto;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.AuthnService;
import org.univcabi.univcabi.auth.service.TokenService;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final AuthnService authnService;
    private final JwtTokenProvider jwtTokenProvider;


    @PostMapping("/authn/token/access")
    public ResponseEntity<AuthnLoginResponseDto> refreshAccessToken(@CookieValue(value = "refreshToken", required = false)String refreshToken){
        if(refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)){
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthnLoginResponseDto.builder().message("유효하지 않은 RefreshToken").build());
        }

        String studentNumber = jwtTokenProvider.getStudentNumberFromToken(refreshToken);
        String storedToken = tokenService.getRefreshToken(studentNumber);

        if(storedToken == null || !storedToken.equals(refreshToken))
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthnLoginResponseDto.builder().message("RefreshToken 불일치").build());
        }

        AuthnRole role = authnService.getUserRole(studentNumber);

        String newAccessToken = jwtTokenProvider.generateAccessToken(studentNumber,role);

        return ResponseEntity.ok(AuthnLoginResponseDto.builder()
                .message("새로운 AccessToken 발급")
                .accessToken(newAccessToken)
                .build());
    }

}
