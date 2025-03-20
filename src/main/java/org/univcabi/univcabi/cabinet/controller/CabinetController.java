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
    public ResponseEntity<CabinetFindAllInfoResponseDto> findAllCabinetInfo(
            @ModelAttribute @Valid CabinetFindAllInfoRequestDto requestDto,
            HttpServletRequest request) {

        // 1. DTO에서 VO로 변환
        CabinetFindAllVo requestVo = new CabinetFindAllVo(
                requestDto.getPage(),
                requestDto.getPageSize()
        );

        // 2-1. 먼저 캐비닛 목록과 페이지 정보 조회
        Page<CabinetVo> cabinetVoPage = cabinetService.findAllCabinetInfo(
                requestVo
        );

        // 2-2. 조회 결과를 기반으로 응답 VO 생성
        CabinetPageResponseVo responseVo = cabinetService.convertToPageResponseVo(
                cabinetVoPage,
                requestVo,
                request
        );

        // CabinetVo 리스트를 CabinetInfoResponseDto 리스트로 변환
        List<CabinetInfoResponseDto> cabinetInfoList = responseVo.results().stream()
                .map(cabinetVo -> {
                    // CabinetVo를 CabinetInfoResponseDto로 명시적 타입 캐스팅 및 변환
                    CabinetVo cabinet = (CabinetVo) cabinetVo;
                    return CabinetInfoResponseDto.builder()
                            .building(cabinet.buildingName())
                            .cabinetNumber(cabinet.cabinetNumber())
                            .floor(cabinet.floor())
                            .build();
                })
                .collect(Collectors.toList());

        // 응답 DTO 생성
        CabinetFindAllInfoResponseDto responseDto = CabinetFindAllInfoResponseDto.of(
                responseVo.count(),
                responseVo.next(),
                responseVo.previous(),
                cabinetInfoList
        );

        // 3. 응답 반환
        return ResponseEntity.ok(responseDto);
    }


    //TODO: JWT userID 사용 추가
    @GetMapping("/detail")
    public ResponseEntity<CabinetFindOneInfoResponseDto> findOneCabinetInfo(@ModelAttribute @Valid CabinetFindOneInfoRequestDto requestDto) {
        // Builder 대신 정적 팩토리 메서드 사용
        CabinetFindOneVo requestVo = new CabinetFindOneVo(requestDto.getCabinetId());

        // Optional 처리
        CabinetDetailVo cabinetDetailVo = cabinetService.findOneCabinetInfo(requestVo)
                .orElseThrow(() -> new EntityNotFoundException("해당 캐비닛을 찾을 수 없습니다. ID: " + requestDto.getCabinetId()));

        CabinetFindOneInfoResponseDto responseDto = CabinetFindOneInfoResponseDto.builder()
                .floor(cabinetDetailVo.floor())
                .section(cabinetDetailVo.section())
                .building(cabinetDetailVo.building())
                .cabinetNumber(cabinetDetailVo.cabinetNumber())
                .status(cabinetDetailVo.status())
                .isVisible(cabinetDetailVo.isVisible())
                .username(cabinetDetailVo.username())
                .isMine(cabinetDetailVo.isMine())
                .expiredAt(cabinetDetailVo.expiredAt())
                .build();

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/rent")
    public ResponseEntity<?> rentCabinet(@RequestBody @Valid CabinetRentRequestDto requestDto) {
        CabinetRentVo requestVo = CabinetRentVo.builder()
                .cabinetId(requestDto.getCabinetId())
                .build();

        CabinetVO cabinetVO = cabinetService.rentCabinet(requestVo);

        // VO를 DTO로 변환
        CabinetRentResponseDto responseDto = new CabinetRentResponseDto(
                cabinetVO.getId(),
                cabinetVO.getCabinetNumber(),
                cabinetVO.getStatus(),
                "대여가 완료되었습니다."
        );

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnCabinet(@RequestBody @Valid CabinetReturnRequestDto requestDto) {
        CabinetReturnVo requestVo = CabinetReturnVo.builder()
                .cabinetId(requestDto.getCabinetId())
                .build();

        CabinetVO cabinetVO = cabinetService.returnCabinet(requestVo);

        // VO를 DTO로 변환
        CabinetReturnResponseDto responseDto = new CabinetReturnResponseDto(
                cabinetVO.getId(),
                cabinetVO.getCabinetNumber(),
                cabinetVO.getStatus(),
                "반납이 완료되었습니다."
        );

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCabinetByKeyword(@ModelAttribute @Valid CabinetSearchRequestDto requestDto) {
        CabinetSearchVo requestVo = CabinetSearchVo.builder()
                .keyword(requestDto.getKeyword())
                .build();

        List<CabinetVO> cabinetVOs = cabinetService.searchCabinetByKeyword(requestVo);

        // VO 목록을 DTO 목록으로 변환
        List<CabinetSearchResponseDto> responseList = cabinetVOs.stream()
                .map(vo -> new CabinetSearchResponseDto(
                        vo.getId(),
                        vo.getCabinetNumber(),
                        vo.getStatus(),
                        vo.getBuildingName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/search/detail")
    public ResponseEntity<?> searchCabinetDetailByKeyword(@ModelAttribute @Valid CabinetSearchDetailRequestDto requestDto) {
        CabinetSearchVo requestVo = CabinetSearchVo.builder()
                .keyword(requestDto.getKeyword())
                .build();

        List<CabinetVO> cabinetVOs = cabinetService.searchCabinetByKeyword(requestVo);

        // VO 목록을 상세 DTO로 변환
        List<CabinetDetailDto> cabinetDetails = cabinetVOs.stream()
                .map(vo -> new CabinetDetailDto(
                        vo.getId(),
                        vo.getCabinetNumber(),
                        vo.getStatus(),
                        vo.getBuildingName(),
                        vo.getPayable()
                        // 필요한 추가 정보
                ))
                .collect(Collectors.toList());

        CabinetSearchDetailResponseDto responseDto = new CabinetSearchDetailResponseDto(
                cabinetDetails,
                cabinetVOs.size()
        );

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/history")
    public ResponseEntity<?> findCabinetRentHistory(@ModelAttribute @Valid CabinetRentHistoryRequestDto requestDto) {
        CabinetRentHistoryVo requestVo = CabinetRentHistoryVo.builder()
                .studentNumber(requestDto.getStudentNumber())
                .build();

        List<CabinetRentHistoryVO> historyVOs = cabinetService.findCabinetRentHistory(requestVo);

        // VO 목록을 DTO로 변환
        List<CabinetHistoryDto> historyItems = historyVOs.stream()
                .map(vo -> new CabinetHistoryDto(
                        vo.getId(),
                        vo.getCabinetId(),
                        vo.getCabinetNumber(),
                        vo.getStartDate(),
                        vo.getEndDate(),
                        vo.getStatus()
                ))
                .collect(Collectors.toList());

        CabinetRentHistoryResponseDto responseDto = new CabinetRentHistoryResponseDto(
                historyItems,
                historyVOs.size()
        );

        return ResponseEntity.ok(responseDto);
    }
}