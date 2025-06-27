package org.univcabi.univcabi.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.user.dto.request.AdminUserCreateRequestDto;
import org.univcabi.univcabi.user.service.UserService;
import org.univcabi.univcabi.user.vo.AdminUserCreateVo;

@RestController
@RequestMapping("/user/admin")
@RequiredArgsConstructor
@Tag(name="관리자 회원 정보 관리")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @PostMapping("/user/create")
    @Operation(summary = "관리자 생성")
    public ResponseEntity<Void> createAdminUser(@RequestBody @Valid AdminUserCreateRequestDto requestDto){

        AdminUserCreateVo requestVo = new AdminUserCreateVo(
                requestDto.getName(),
                requestDto.getAffiliation(),
                requestDto.getPhoneNumber(),
                requestDto.getStudentNumber(),
                requestDto.getPassword(),
                AuthnRole.ADMIN,
                requestDto.getBuildingName(),
                requestDto.getFloor(),
                requestDto.getSection()
        );

        userService.createAdminUser(requestVo);

        return ResponseEntity.ok().build();
    }
}
