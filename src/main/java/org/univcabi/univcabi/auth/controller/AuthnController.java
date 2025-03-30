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
import org.univcabi.univcabi.auth.dto.response.AuthnCreateResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnDeleteResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnResponseDto;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.AuthnService;
import org.univcabi.univcabi.auth.service.TokenService;
import org.univcabi.univcabi.auth.vo.AuthnCreateVo;
import org.univcabi.univcabi.auth.vo.AuthnDeleteVo;

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
    public ResponseEntity<AuthnResponseDto> login(@RequestBody AuthnCreateRequestDto requestDto){
        try{
            AuthnResponseDto responseDto = authnService.login(requestDto);

            String accessToken = jwtTokenProvider.generateAccessToken(responseDto.getStudentNumber(),"USER");
            String refreshToken = jwtTokenProvider.generateRefreshToken(responseDto.getStudentNumber());

            tokenService.storeRefreshToken(responseDto.getStudentNumber(),refreshToken);

            ResponseCookie refreshCookie = tokenService.createRefreshTokenCookie(refreshToken);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE,refreshCookie.toString())
                    .body(AuthnResponseDto.builder()
                            .studentNumber(responseDto.getStudentNumber())
                            .message("로그인 성공")
                            .accessToken(accessToken)
                            .build());


        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthnResponseDto.builder().message("존재하지 않는 유저입니다.").build());
        }catch (SecurityException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthnResponseDto.builder().message("비밀번호가 옳바르지 않습니다.").build());
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AuthnResponseDto.builder().message("서버 오류").build());
        }
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
