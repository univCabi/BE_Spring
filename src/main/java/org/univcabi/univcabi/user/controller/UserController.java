package org.univcabi.univcabi.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.auth.security.JwtTokenProvider;
import org.univcabi.univcabi.user.dto.response.UserProfileResponseDto;
import org.univcabi.univcabi.user.service.UserService;
import org.univcabi.univcabi.user.vo.UserProfileVo;

import static org.univcabi.univcabi.util.TokenUtils.resolveToken;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @GetMapping("/profile/me")
    public ResponseEntity<UserProfileResponseDto> getMyProfile(HttpServletRequest request){
        String token = resolveToken(request);
        String studentNumber = jwtTokenProvider.getStudentNumberFromToken(token);

        // studentNumber 로
        UserProfileVo requestVo = userService.getUserProfileByStudentNumber(studentNumber);

        UserProfileResponseDto responseDto = UserProfileResponseDto.of(requestVo);

        return ResponseEntity.ok(responseDto);
    }
}
