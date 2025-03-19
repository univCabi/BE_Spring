package org.univcabi.univcabi.cabinet.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.cabinet.dto.response.*;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.dto.request.*;
import org.univcabi.univcabi.cabinet.vo.*;

//TODO: jwt verify 과정 후 decode를 통해서 studentNumber 사용

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
        CabinetFindAllVO cabinetFindAllVO = CabinetFindAllVO.builder()
                .page(cabinetFindAllInfoRequestDto.getPage())
                .size(cabinetFindAllInfoRequestDto.getPageSize())
                .build();

        CabinetFindAllInfoResponseDto cabinetFindAllInfoResponseDto = cabinetService.findAllCabinetInfo(cabinetFindAllVO);
        return ResponseEntity.ok(cabinetFindAllInfoResponseDto);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> findOneCabinetInfo(@ModelAttribute @Valid CabinetFindOneInfoRequestDto cabinetFindOneInfoRequestDto) {
        CabinetDetailVO cabinetDetailVO = CabinetDetailVO.builder()
                .cabinetId(cabinetFindOneInfoRequestDto.getCabinetId())
                .build();

        CabinetFindOneInfoResponseDto cabinetFindOneInfoResponseDto = cabinetService.findOneCabinetInfo(cabinetDetailVO);
        return ResponseEntity.ok(cabinetFindOneInfoResponseDto);
    }

    @PostMapping("/rent")
    public ResponseEntity<?> rentCabinet(@RequestBody @Valid CabinetRentRequestDto cabinetRentRequestDto) {
        CabinetRentVO cabinetRentVO = CabinetRentVO.builder()
                .cabinetId(cabinetRentRequestDto.getCabinetId())
                .build();

        CabinetRentResponseDto cabinetRentResponseDto = cabinetService.rentCabinet(cabinetRentVO);
        return ResponseEntity.ok(cabinetRentResponseDto);
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnCabinet(@RequestBody @Valid CabinetReturnRequestDto requestDto) {
        CabinetReturnVO cabinetReturnVO = CabinetReturnVO.builder()
                .cabinetId(requestDto.getCabinetId())
                .build();

        CabinetReturnResponseDto cabinetReturnRequestDto = cabinetService.returnCabinet(cabinetReturnVO);
        return ResponseEntity.ok(cabinetReturnRequestDto);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCabinetByKeyword(@ModelAttribute @Valid CabinetSearchRequestDto requestDto) {
        CabinetSearchVO cabinetSearchVO = CabinetSearchVO.builder()
                .keyword(requestDto.getKeyword())
                .build();

        CabinetSearchResponseDto cabinetSearchRequestDto = cabinetService.searchCabinetByKeyword(cabinetSearchVO);
        return ResponseEntity.ok(cabinetSearchRequestDto);
    }

    @GetMapping("/search/detail")
    public ResponseEntity<?> searchCabinetDetailByKeyword(@ModelAttribute @Valid CabinetSearchDetailRequestDto requestDto) {
        // 검색 세부 사항을 포함한 VO 생성
        CabinetSearchVO cabinetSearchVO = CabinetSearchVO.builder()
                .keyword(requestDto.getKeyword())
                .build();

        CabinetSearchDetailResponseDto cabinetSearchDetailResponseDto = cabinetService.searchCabinetByKeyword(cabinetSearchVO);
        return ResponseEntity.ok(cabinetSearchDetailResponseDto);
    }

    @GetMapping("/history")
    public ResponseEntity<?> findCabinetRentHistory(@ModelAttribute @Valid CabinetRentHistoryRequestDto requestDto) {
        CabinetRentHistoryVO cabinetRentHistoryVO = CabinetRentHistoryVO.builder()
                .studentNumber(requestDto.getStudentNumber())
                .build();

        CabinetRentHistoryResponseDto cabinetRentHistoryRequestDto = cabinetService.findCabinetRentHistory(cabinetRentHistoryVO);
        return ResponseEntity.ok(cabinetRentHistoryRequestDto);
    }
}