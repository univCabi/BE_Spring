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
import org.univcabi.univcabi.user.dto.request.AdminUserCreateRequestDto;
import org.univcabi.univcabi.user.service.UserService;

@RestController
@RequestMapping("/user/admin")
@RequiredArgsConstructor
@Tag(name="관리자 회원 정보 관리")
public class AdminUserController {

    private final UserService userService;

    @PostMapping("/user/create")
    @Operation(summary = "관리자 생성")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createAdminUser(@RequestBody @Valid AdminUserCreateRequestDto requestDto){

        return ResponseEntity.ok().build();
    }
}
