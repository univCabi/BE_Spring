package org.univcabi.univcabi.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.auth.dto.request.AuthnCreateRequestDto;
import org.univcabi.univcabi.auth.dto.request.AuthnDeleteRequestDto;
import org.univcabi.univcabi.auth.dto.request.AuthnLoginRequestDto;
import org.univcabi.univcabi.auth.dto.response.AuthnCreateResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnDeleteResponseDto;
import org.univcabi.univcabi.auth.dto.response.AuthnLoginResponseDto;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.AuthnService;
import org.univcabi.univcabi.auth.service.TokenService;
import org.univcabi.univcabi.auth.vo.AuthnCreateVo;
import org.univcabi.univcabi.auth.vo.AuthnDeleteVo;
import org.univcabi.univcabi.auth.vo.AuthnLoginVo;
import org.univcabi.univcabi.auth.vo.AuthnTokenGenerateVo;
import org.univcabi.univcabi.exception.ControllerException;

import static org.univcabi.univcabi.exception.ExceptionStatus.AUTH_INVALID_PARAMS;
import static org.univcabi.univcabi.exception.ExceptionStatus.AUTH_SESSION_UNAUTHORIZED;

@Slf4j
@RestController
@RequestMapping("/authn")
@RequiredArgsConstructor
@Tag(name="회원 로직")
public class AuthnController {

    private final AuthnService authnService;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    @Operation(summary = "회원가입")
    public ResponseEntity<AuthnCreateResponseDto> createUser(@RequestBody @Valid AuthnCreateRequestDto requestDto){
        // studentNumber, password, role 파라미터로 User 생성
        AuthnCreateVo requestVo = new AuthnCreateVo(
                requestDto.getStudentNumber(),
                requestDto.getPassword(),
                requestDto.getRole()
        );

        AuthnCreateVo responseVo = authnService.createUser(requestVo);

        AuthnCreateResponseDto responseDto = AuthnCreateResponseDto.builder()
                .studentNumber(responseVo.studentNumber())
                .message("회원 생성 성공")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/delete")
    @Operation(summary="회원탈퇴")
    public  ResponseEntity<AuthnDeleteResponseDto> deleteUserByStudentNumber(@RequestBody @Valid AuthnDeleteRequestDto requestDto){
        // studentNumber 로 User 삭제
        AuthnDeleteVo requestVo = new AuthnDeleteVo(
                requestDto.getStudentNumber()
        );
        // 삭제는 SoftDelete 방식 ( deleted_at의 값을 null -> LocalDate.now() )
        AuthnDeleteVo responseVo = authnService.softDeleteUserByStudentNumber(requestVo);

        AuthnDeleteResponseDto responseDto = AuthnDeleteResponseDto.builder()
                .studentNumber(responseVo.studentNumber())
                .message("회원 삭제 성공")
                .build();
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/login")
    @Operation(summary="로그인")
    public ResponseEntity<AuthnLoginResponseDto> loginByStudentNumberAndPassword(@RequestBody @Valid AuthnLoginRequestDto requestDto){
        AuthnLoginVo requestVo = new AuthnLoginVo(
                requestDto.getStudentNumber(),
                requestDto.getPassword()
        );

        AuthnTokenGenerateVo tokenVo = authnService.loginByStudentNumberAndPassword(requestVo);

        String accessToken = jwtTokenProvider.generateAccessToken(
                tokenVo.studentNumber(),
                tokenVo.role());

        String refreshToken = jwtTokenProvider.generateRefreshToken(requestDto.getStudentNumber());

        // redis에 refreshToken 저장
        tokenService.storeRefreshToken(tokenVo.studentNumber(),refreshToken);

        ResponseCookie refreshCookie = tokenService.createRefreshTokenCookie(refreshToken);
        // 로그인 성공시 accessToken 발급 ( 응답의 body 값 )
        AuthnLoginResponseDto responseDto = AuthnLoginResponseDto.builder().accessToken(accessToken).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,refreshCookie.toString())
                .body(responseDto);

    }

    @PostMapping("/logout")
    @Operation(summary="로그아웃")
    public ResponseEntity<String> logoutByJwtToken(){
        Authentication authn = SecurityContextHolder.getContext().getAuthentication();

        // 잘못된 세션
        if(authn==null) {
            throw new ControllerException(AUTH_SESSION_UNAUTHORIZED);
        }

        Object principal = authn.getPrincipal();

        // 인증 객체가 유효하지 않음
        if(!(principal instanceof UserDetails)){
            throw new ControllerException(AUTH_INVALID_PARAMS);
        }

        UserDetails userDetails = (UserDetails) principal;
        String studentNumber = userDetails.getUsername();

        //해당 사용자의 contextHolder 정보 삭제
        SecurityContextHolder.clearContext();
        //해당 사용자의 redis의 refreshToken 정보 삭제
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
