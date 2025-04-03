package org.univcabi.univcabi.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.user.dto.request.UserVisibilityRequestDto;
import org.univcabi.univcabi.user.dto.response.UserProfileResponseDto;
import org.univcabi.univcabi.user.service.UserService;
import org.univcabi.univcabi.user.vo.UserProfileVo;
import org.univcabi.univcabi.user.vo.UserVisibilityVo;
import org.springframework.security.core.Authentication;


import static org.univcabi.univcabi.util.TokenUtils.resolveToken;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    // User profile 에 필요한 정보 조회
    @GetMapping("/profile/me")
    public ResponseEntity<UserProfileResponseDto> getMyProfile(Authentication authentication){
        String studentNumber = authentication.getName();

        UserProfileVo requestVo = userService.getUserProfileByStudentNumber(studentNumber);

        UserProfileResponseDto responseDto = UserProfileResponseDto.of(requestVo);

        return ResponseEntity.ok(responseDto);
    }

    // isVisibility 속성 변경
    @PostMapping("/profile/me")
    public ResponseEntity<Void> updateUserVisibility(@RequestBody @Valid UserVisibilityRequestDto requestDto,Authentication authentication){
        String studentNumber = authentication.getName();

        // isVisibility 속성과 JWT로부터 얻은 studentNumber 로 requestVo 생성
        UserVisibilityVo requestVo = new UserVisibilityVo(studentNumber,requestDto.getIsVisible());
        userService.updateUserVisibility(requestVo);

        return ResponseEntity.ok().build();
    }


    // 데이터 베이스 초기화
    @PostMapping("/mockup")
    public ResponseEntity<Void> resetDataBase(){
        userService.resetDatabase();

        return ResponseEntity.ok().build();
    }
}
