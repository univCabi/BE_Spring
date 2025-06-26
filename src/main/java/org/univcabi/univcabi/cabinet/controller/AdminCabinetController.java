package org.univcabi.univcabi.cabinet.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.cabinet.dto.request.CabinetAdminChangeStatusRequestDto;
import org.univcabi.univcabi.cabinet.dto.request.CabinetReturnListRequestDto;
import org.univcabi.univcabi.cabinet.dto.response.CabinetAdminChangeStatusResponseDto;
import org.univcabi.univcabi.cabinet.dto.response.CabinetReturnDataResponseDto;
import org.univcabi.univcabi.cabinet.dto.response.CabinetReturnResultResponseDto;
import org.univcabi.univcabi.cabinet.dto.response.CabinetStatusCountResponseDto;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.vo.*;

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
    public ResponseEntity<CabinetReturnResultResponseDto> returnCabinetsByCabinetIds(@RequestBody @Valid CabinetReturnListRequestDto requestDto){
        CabinetReturnResultVo returnResultVo = cabinetService.returnCabinetsByCabinetIds(
                new CabinetReturnCabinetIdsVo(requestDto.getCabinetIds()));

        List<CabinetReturnDataResponseDto> returnDataResponseDtoList = returnResultVo.listVos().stream()
                .map(vo -> CabinetReturnDataResponseDto.builder()
                        .id(vo.id())
                        .buildingName(vo.buildingName())
                        .cabinetNumber(vo.cabinetNumber())
                        .status(vo.status())
                        .name(vo.name())
                        .build()
                ).toList();

        return ResponseEntity.ok(
            CabinetReturnResultResponseDto.builder()
                    .dtoList(returnDataResponseDtoList)
                    .message(returnResultVo.message())
                    .errors(returnResultVo.errors())
                    .build()
        );
    }

    @PostMapping("/change/status")
    @Operation(summary = "사물함 상태값을 변경 후 사물함 정보 반환")
    public ResponseEntity<CabinetAdminChangeStatusResponseDto> changeCabinetStatusByCabinetIdsAndNewStatus(@RequestBody @Valid CabinetAdminChangeStatusRequestDto requestDto){
        CabinetAdminChangeStatusVo requestVo = new CabinetAdminChangeStatusVo(
                requestDto.getCabinetIds(),
                requestDto.getNewStatus(),
                requestDto.getReason(),
                requestDto.getStudentNumber()
        );

        CabinetAdminChangeStatusResultVo resultVo = cabinetService.changeCabinetStatusByCabinetIdsAndNewStatus(requestVo);

        CabinetAdminChangeStatusResponseDto responseDto = CabinetAdminChangeStatusResponseDto.builder()
                .cabinets(resultVo.cabinetStatusInfoVos())
                .message(resultVo.message())
                .build();

        return ResponseEntity.ok(responseDto);
    }
}