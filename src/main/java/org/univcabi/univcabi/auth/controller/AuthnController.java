package org.univcabi.univcabi.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.auth.dto.request.AuthnCreateRequestDto;
import org.univcabi.univcabi.auth.dto.request.AuthnDeleteRequestDto;
import org.univcabi.univcabi.auth.dto.request.AuthnLoginRequestDto;
import org.univcabi.univcabi.auth.dto.response.AuthnCreateResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnDeleteResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnLoginResponseDto;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.AuthnService;
import org.univcabi.univcabi.auth.service.TokenService;
import org.univcabi.univcabi.auth.vo.AuthnCreateVo;
import org.univcabi.univcabi.auth.vo.AuthnDeleteVo;
import org.univcabi.univcabi.auth.vo.AuthnLoginVo;
import org.univcabi.univcabi.auth.vo.AuthnTokenGenerateVo;

@Slf4j
@RestController
@RequestMapping("/authn")
@RequiredArgsConstructor
public class AuthnController {

    private final AuthnService authnService;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    public ResponseEntity<AuthnCreateResponseDto> createUser(@RequestBody @Valid AuthnCreateRequestDto requestDto){

        AuthnCreateVo requestVo = new AuthnCreateVo(
                requestDto.getStudentNumber(),
                requestDto.getPassword(),
                AuthnRole.NORMAL
        );

        AuthnCreateResponseDto responseDto = authnService.createUser(requestVo);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/delete")
    public  ResponseEntity<AuthnDeleteResponseDto> deleteUser(@RequestBody @Valid AuthnDeleteRequestDto requestDto){

        AuthnDeleteVo requestVo = new AuthnDeleteVo(
                requestDto.getStudentNumber()
        );

        AuthnDeleteResponseDto responseDto = authnService.deleteUser(requestVo);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthnLoginResponseDto> login(@RequestBody @Valid AuthnLoginRequestDto requestDto){
        AuthnLoginVo requestVo = new AuthnLoginVo(
                requestDto.getStudentNumber(),
                requestDto.getPassword()
        );

        AuthnTokenGenerateVo tokenVo = authnService.login(requestVo);

        String accessToken = jwtTokenProvider.generateAccessToken(
                tokenVo.studentNumber(),
                tokenVo.role());

        String refreshToken = jwtTokenProvider.generateRefreshToken(requestDto.getStudentNumber());

        ResponseCookie refreshCookie = tokenService.createRefreshTokenCookie(refreshToken);

        AuthnLoginResponseDto responseDto = AuthnLoginResponseDto.of(accessToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,refreshCookie.toString())
                .body(responseDto);

    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(){
        if(SecurityContextHolder.getContext().getAuthentication()==null) {
            return ResponseEntity.badRequest().body("인증되지 않은 요청입니다.");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!(principal instanceof UserDetails)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 사용자입니다.");
        }

        UserDetails userDetails = (UserDetails) principal;
        String studentNumber = userDetails.getUsername();

        SecurityContextHolder.clearContext();

        tokenService.deleteRefreshToken(studentNumber);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken","")
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,deleteCookie.toString())
                .body("로그아웃 성공");
    }

}
