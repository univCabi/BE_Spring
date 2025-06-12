package org.univcabi.univcabi.cabinet.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.univcabi.univcabi.cabinet.dto.response.CabinetStatusCountResponseDto;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.service.CabinetService;

import java.util.List;

@RestController
@RequestMapping("/cabinet/admin")
@Tag(name = "ADMIN 사용자 사물함 관리 로직")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCabinetController {
    private final CabinetService cabinetService;

    @GetMapping("/dashboard")
    @Operation(summary = "건물별 사물함 사용 현황")
    public ResponseEntity<CabinetStatusCountResponseDto> getCabinetStatusCountsGroupByBuilding(){

        CabinetStatusCountResponseDto responseDto = CabinetStatusCountResponseDto.builder()
                .vos(cabinetService.getCabinetStatusCountsGroupByBuilding())
                .build();

        return ResponseEntity.ok(responseDto);
    }


    @PostMapping("/return")
    @Operation(summary = "반납할 사물함 id를 보내고 해당 사물함의 상태 값을 반환")
    public ResponseEntity<?>
}