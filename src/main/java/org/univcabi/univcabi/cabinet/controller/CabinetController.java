package org.univcabi.univcabi.cabinet.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.cabinet.dto.response.*;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.dto.request.*;
import org.univcabi.univcabi.cabinet.vo.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cabinet")
public class CabinetController {
    private final CabinetService cabinetService;

    public CabinetController(CabinetService cabinetService) {
        this.cabinetService = cabinetService;
    }

    @GetMapping("/all")
    public ResponseEntity<?> findAllCabinetInfo(@ModelAttribute @Valid CabinetFindAllInfoRequestDto cabinetFindAllInfoRequestDto) {
        // DTO를 VO로 변환 (Builder 패턴 적용)
        CabinetFindAllVo cabinetFindAllVo = CabinetFindAllVo.builder()
                .page(cabinetFindAllInfoRequestDto.getPage())
                .size(cabinetFindAllInfoRequestDto.getPageSize())
                .build();

        Page<CabinetFindAllInfoResponseDto> cabinetFindAllInfoResponseDto = cabinetService.findAllCabinetInfo(cabinetFindAllVo);
        return ResponseEntity.ok(cabinetFindAllInfoResponseDto);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> findOneCabinetInfo(@ModelAttribute @Valid CabinetFindOneInfoRequestDto cabinetFindOneInfoRequestDto) {
        CabinetDetailVo cabinetDetailVo = CabinetDetailVo.builder()
                .cabinetId(cabinetFindOneInfoRequestDto.getCabinetId())
                .build();

        CabinetFindOneInfoResponseDto cabinetFindOneInfoResponseDto = cabinetService.findOneCabinetInfo(cabinetDetailVo);
        return ResponseEntity.ok(cabinetFindOneInfoResponseDto);
    }

    @PostMapping("/rent")
    public ResponseEntity<?> rentCabinet(@RequestBody @Valid CabinetRentRequestDto cabinetRentRequestDto) {
        CabinetRentVo cabinetRentVo = CabinetRentVo.builder()
                .cabinetId(cabinetRentRequestDto.getCabinetId())
                .build();

        CabinetRentResponseDto cabinetRentResponseDto = cabinetService.rentCabinet(cabinetRentVo);
        return ResponseEntity.ok(cabinetRentResponseDto);
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnCabinet(@RequestBody @Valid CabinetReturnRequestDto requestDto) {
        CabinetReturnVo cabinetReturnVo = CabinetReturnVo.builder()
                .cabinetId(requestDto.getCabinetId())
                .build();

        CabinetReturnResponseDto cabinetReturnRequestDto = cabinetService.returnCabinet(cabinetReturnVo);
        return ResponseEntity.ok(cabinetReturnRequestDto);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCabinetByKeyword(@ModelAttribute @Valid CabinetSearchRequestDto requestDto) {
        CabinetSearchVo cabinetSearchVo = CabinetSearchVo.builder()
                .keyword(requestDto.getKeyword())
                .build();

        CabinetSearchResponseDto cabinetSearchRequestDto = cabinetService.searchCabinetByKeyword(cabinetSearchVo);
        return ResponseEntity.ok(cabinetSearchRequestDto);
    }

    @GetMapping("/search/detail")
    public ResponseEntity<?> searchCabinetDetailByKeyword(@ModelAttribute @Valid CabinetSearchDetailRequestDto requestDto) {
        // 검색 세부 사항을 포함한 VO 생성
        CabinetSearchVo cabinetSearchVo = CabinetSearchVo.builder()
                .keyword(requestDto.getKeyword())
                .build();

        CabinetSearchDetailResponseDto cabinetSearchDetailResponseDto = cabinetService.searchCabinetByKeyword(cabinetSearchVo);
        return ResponseEntity.ok(cabinetSearchDetailResponseDto);
    }

    @GetMapping("/history")
    public ResponseEntity<?> findCabinetRentHistory(@ModelAttribute @Valid CabinetRentHistoryRequestDto requestDto) {
        CabinetRentHistoryVo cabinetRentHistoryVo = CabinetRentHistoryVo.builder()
                .studentNumber(requestDto.getStudentNumber())
                .build();

        CabinetRentHistoryResponseDto cabinetRentHistoryRequestDto = cabinetService.findCabinetRentHistory(cabinetRentHistoryVo);
        return ResponseEntity.ok(cabinetRentHistoryRequestDto);
    }
}