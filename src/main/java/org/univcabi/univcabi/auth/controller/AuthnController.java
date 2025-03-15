package org.univcabi.univcabi.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.auth.dto.AuthnRequestDto;
import org.univcabi.univcabi.auth.dto.AuthnResponseDto;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.auth.service.AuthnService;

@RestController
@RequestMapping("/authn")
@RequiredArgsConstructor
public class AuthnController {

    private final AuthnService authnService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    public ResponseEntity<String> createUser(){
        authnService.createUser(null); // Test 를 위해 null 사용
        return ResponseEntity.status(201).body("회원 생성 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthnResponseDto> login(@RequestBody AuthnRequestDto requestDto, HttpServletResponse response){
        try{
            AuthnResponseDto responseDto = authnService.login(requestDto);

            String accessToken = jwtTokenProvider.generateAccessToken(responseDto.getStudentNumber(),"USER");
            String refreshToken = jwtTokenProvider.generateRefreshToken(responseDto.getStudentNumber());

            authnService.storeRefreshToken(requestDto.getStudentNumber(),refreshToken);

            Cookie accessTokenCookie = new Cookie("access_token",accessToken);
            Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(true);
            accessTokenCookie.setPath("/");

            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setPath("/");

            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);

            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(404).body(AuthnResponseDto.builder().message("존재하지 않는 유저입니다.").build());
        }catch (SecurityException e){
            return ResponseEntity.status(400).body(AuthnResponseDto.builder().message("비밀번호가 옳바르지 않습니다.").build());
        }catch (Exception e){
            return ResponseEntity.status(500).body(AuthnResponseDto.builder().message("서버 오류").build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response){
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!(principal instanceof UserDetails)){
            return ResponseEntity.status(401).body("유효하지 않은 사용자입니다.");
        }

        UserDetails userDetails = (UserDetails) principal;
        String studentNumber = userDetails.getUsername();

        authnService.deleteRefreshToken(studentNumber);

        deleteCookie(response,"access_token");
        deleteCookie(response,"refresh_token");

        return ResponseEntity.ok("로그아웃 성공");
    }

    private  void deleteCookie(HttpServletResponse response,String cookieName){
        Cookie cookie = new Cookie(cookieName,null);
        cookie.setMaxAge(0); // 쿠키 만료 -> 브라우저는 만료된 쿠키 삭제
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    @PostMapping("/delete")
    public  ResponseEntity<String> deleteUser(){
        authnService.deleteUser(null);
        return ResponseEntity.status(201).body("회원 삭제 성공");
    }
}
