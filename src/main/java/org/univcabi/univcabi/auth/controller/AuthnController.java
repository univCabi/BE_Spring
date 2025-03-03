package org.univcabi.univcabi.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.auth.dto.AuthnRequestDto;
import org.univcabi.univcabi.auth.dto.AuthnResponseDto;
import org.univcabi.univcabi.auth.service.AuthnService;

@RestController
@RequestMapping("/authn")
@RequiredArgsConstructor
public class AuthnController {

    private final AuthnService authnService;

    @PostMapping("/create")
    public ResponseEntity<String> createUser(){
        authnService.createUser(null); // Test 를 위해 null 사용
        return ResponseEntity.status(201).body("회원 생성 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthnResponseDto> login(@RequestBody AuthnRequestDto requestDto){
        try{
            AuthnResponseDto responseDto = authnService.login(requestDto);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(404).body(new AuthnResponseDto("존재하지 않는 유저입니다."));
        }catch (SecurityException e){
            return ResponseEntity.status(400).body(new AuthnResponseDto("비밀번호가 옳바르지 않습니다."));

        }catch (Exception e){
            return ResponseEntity.status(500).body(new AuthnResponseDto("서버 오류"));
        }
    }

    @PostMapping("/delete")
    public  ResponseEntity<String> deleteUser(){
        authnService.deleteUser(null);
        return ResponseEntity.status(201).body("회원 삭제 성공");
    }
}
