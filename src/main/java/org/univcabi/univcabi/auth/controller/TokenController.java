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
    public ResponseEntity<AuthnLoginResponseDto> refreshAccessToken(@CookieValue(value = "refreshToken")String refreshToken){
        String studentNumber = jwtTokenProvider.getStudentNumberFromToken(refreshToken);

        AuthnRole role = authnService.getUserRole(studentNumber);

        String newAccessToken = jwtTokenProvider.generateAccessToken(studentNumber,role);

        return ResponseEntity.ok(AuthnLoginResponseDto.builder()
                .accessToken(newAccessToken)
                .build());
    }

}
