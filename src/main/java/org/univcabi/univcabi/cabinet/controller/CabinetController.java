package org.univcabi.univcabi.cabinet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.cabinet.dto.response.*;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.QBuilding;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.dto.request.*;
import org.univcabi.univcabi.cabinet.service.CabinetUtilService;
import org.univcabi.univcabi.cabinet.vo.*;
import org.univcabi.univcabi.exception.AsyncException;
import org.univcabi.univcabi.exception.ExceptionStatus;
import org.univcabi.univcabi.exception.RepositoryException;
import org.univcabi.univcabi.exception.ServiceException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static org.univcabi.univcabi.exception.ExceptionStatus.*;

@RestController
@RequestMapping("/cabinet")
@Tag(name="cabinet 로직")
public class CabinetController {
    private final CabinetService cabinetService;
    private final CabinetUtilService cabinetUtilService;

    public CabinetController(CabinetService cabinetService, CabinetUtilService cabinetUtilService) {
        this.cabinetService = cabinetService;
        this.cabinetUtilService = cabinetUtilService;
    }

    @GetMapping
    @Operation(summary = "빌딩 과 층에 해당하는 사물함들 정보 조회")
    public ResponseEntity<List<CabinetDataResponseDto>> findCabinetsByBuildingAndFloor(
            @ModelAttribute @Valid CabinetLocationRequestDto requestDto,
            Authentication authentication
            ){

        CabinetLocationVo requestVo = new CabinetLocationVo(
                BuildingName.valueOf(requestDto.getBuilding()),
                requestDto.getFloors(),
                authentication.getName());

        List<CabinetDataVo> cabinetDataVoList = cabinetService.findCabinetsByBuildingAndFloor(requestVo);

        List<CabinetDataResponseDto> cabinetDataResponseDtoList = cabinetDataVoList.stream()
                .map(CabinetDataResponseDto::of)
                .toList();

        return ResponseEntity.ok(cabinetDataResponseDtoList);
    }

    @GetMapping("/all")
    @Operation(summary = "사물함 전체 조회")
    public ResponseEntity<CabinetFindAllInfoResponseDto> findAllCabinetInfo(
            @ModelAttribute @Valid CabinetFindAllInfoRequestDto requestDto,
            HttpServletRequest request) {

        // 1. DTO에서 VO로 변환
        CabinetPageVo requestVo = new CabinetPageVo(
                requestDto.getPage(),
                requestDto.getPageSize()
        );

        // 2-1. 먼저 캐비닛 목록과 페이지 정보 조회
        Page<CabinetVo> cabinetVoPage = cabinetService.findAllCabinetInfo(
                requestVo
        );

        // 2-2. 조회 결과를 기반으로 응답 VO 생성
        CabinetPageResponseVo<CabinetVo> responseVo = cabinetUtilService.convertToPageResponseVo(
                cabinetVoPage,
                requestVo,
                request
        );

        // CabinetVo 리스트를 CabinetInfoResponseDto 리스트로 변환
        List<CabinetInfoResponseDto> cabinetInfoList = responseVo.results().stream()
                .map(cabinet -> CabinetInfoResponseDto.builder()
                        .building(cabinet.buildingName())
                        .cabinetNumber(cabinet.cabinetNumber())
                        .floor(cabinet.floor())
                        .build())
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
    @Operation(summary = "사물함 상세 정보 조회")
    public ResponseEntity<CabinetDetailResponseDto> findOneCabinetInfo(@ModelAttribute @Valid CabinetFindOneInfoRequestDto requestDto) {
        // Builder 대신 정적 팩토리 메서드 사용

        //TODO: 변경필요
        String studentNumber = "202111741";

        CabinetFindOneVo requestVo = new CabinetFindOneVo(requestDto.getCabinetId(), studentNumber);

        // Optional 처리
        CabinetDetailVo cabinetDetailVo = cabinetService.findOneCabinetInfo(requestVo);

        CabinetDetailResponseDto responseDto = CabinetDetailResponseDto.builder()
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
    @Operation(summary = "사물함 대여")
    public CompletableFuture<ResponseEntity<CabinetDetailResponseDto>> rentCabinet(@RequestBody @Valid CabinetRentRequestDto requestDto) {
        String studentNumber = "202111741";
        CabinetRentVo requestVo = new CabinetRentVo(requestDto.getCabinetId(), studentNumber);

        return cabinetService.rentCabinet(requestVo)
                .thenApply(cabinetDetailVo -> {
                    // 성공 응답 생성
                    CabinetDetailResponseDto responseDto = CabinetDetailResponseDto.builder()
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
                })
                .exceptionally(ex -> {
                    // 원인 예외 추출
                    Throwable cause = ex;
                    if (ex instanceof CompletionException && ex.getCause() != null) {
                        cause = ex.getCause();
                    }

                    // 원인 예외에 따라 적절한 AsyncException으로 변환하여 전역 예외 핸들러에게 전달
                    if (cause instanceof ServiceException) {
                        ServiceException serviceEx = (ServiceException) cause;
                        throw new AsyncException(serviceEx.getStatus());
                    } else if (cause instanceof RepositoryException) {
                        RepositoryException repoEx = (RepositoryException) cause;
                        throw new AsyncException(repoEx.getStatus());
                    } else {
                        // 기타 예외는 일반 서버 오류로 처리
                        throw new AsyncException(
                                ExceptionStatus.GENERAL_INTERNAL_SERVER_ERROR
                        );
                    }
                });
    }

    @PostMapping("/return")
    @Operation(summary = "사물함 반납")
    public ResponseEntity<CabinetDetailResponseDto> returnCabinet(@RequestBody @Valid CabinetReturnRequestDto requestDto) {
        //TODO: 변경필요
        String studentNumber = "202111741";

        CabinetReturnVo requestVo = new CabinetReturnVo(requestDto.getCabinetId(), studentNumber);

        CabinetDetailVo cabinetDetailVo = cabinetService.returnCabinet(requestVo);

        CabinetDetailResponseDto responseDto = CabinetDetailResponseDto.builder()
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

    @GetMapping("/search")
    @Operation(summary = "사물함 검색 결과")
    public ResponseEntity<List<CabinetSearchResponseDto>> searchCabinetByKeyword(@ModelAttribute @Valid CabinetSearchRequestDto requestDto) {
        CabinetSearchVo requestVo = new CabinetSearchVo(requestDto.getKeyword());

        List<CabinetVo> cabinetVos = cabinetService.searchCabinetByKeyword(requestVo);

        // VO 목록을 DTO 목록으로 변환
        List<CabinetSearchResponseDto> responseList = cabinetVos.stream()
                .map(vo -> new CabinetSearchResponseDto(
                        vo.buildingName(),
                        vo.floor(),
                        vo.cabinetNumber()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/search/detail")
    @Operation(summary = "사물함 구체적인 검색 결과")
    public ResponseEntity<CabinetFindAllInfoResponseDto> searchCabinetDetailByKeyword(@ModelAttribute @Valid CabinetSearchDetailRequestDto requestDto,
                                                          HttpServletRequest request) {
        // DTO를 VO로 변환 (빌더 패턴 사용 가능하면 좋을 것)
        CabinetSearchDetailVo requestVo = new CabinetSearchDetailVo(
                requestDto.getKeyword(),
                requestDto.getPage(),
                requestDto.getPageSize()
        );

        // 서비스 호출해서 페이징된 결과 가져오기
        Page<CabinetVo> cabinetVoPage = cabinetService.searchDetailByKeyword(requestVo);

        CabinetPageVo pageVo = new CabinetPageVo(
                requestDto.getPage(),
                requestDto.getPageSize()
        );

        // 페이지 응답 VO로 변환 (제네릭 타입 명시)
        CabinetPageResponseVo<CabinetVo> responseVo = cabinetUtilService.convertToPageResponseVo(
                cabinetVoPage,
                pageVo,
                request
        );

        // CabinetVo 리스트를 CabinetInfoResponseDto 리스트로 변환 (캐스팅 불필요)
        List<CabinetInfoResponseDto> cabinetInfoList = responseVo.results().stream()
                .map(cabinet -> CabinetInfoResponseDto.builder()
                        .building(cabinet.buildingName())
                        .cabinetNumber(cabinet.cabinetNumber())
                        .floor(cabinet.floor())
                        .build())
                .collect(Collectors.toList());

        // 응답 DTO 생성 (빌더 패턴 사용)
        CabinetFindAllInfoResponseDto responseDto = CabinetFindAllInfoResponseDto.builder()
                .count(responseVo.count())
                .next(responseVo.next())
                .previous(responseVo.previous())
                .results(cabinetInfoList)
                .build();

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/history")
    @Operation(summary = "사물함 이력 조회")
    public ResponseEntity<List<CabinetHistoryResponseDto>> findCabinetHistory(@ModelAttribute @Valid CabinetHistoryRequestDto requestDto,
                                                HttpServletRequest request) {
        CabinetHistoryVo requestVo = new CabinetHistoryVo(
                requestDto.getStudentNumber(),
                requestDto.getPage(),
                requestDto.getPageSize()
        );

        Page<CabinetHistoryResponseVo> historyVOsPage = cabinetService.findCabinetRentHistory(requestVo);

        // 페이지 응답 VO로 변환 (제네릭 타입 명시)
        CabinetPageVo pageVo = new CabinetPageVo(
                requestDto.getPage(),
                requestDto.getPageSize()
        );

        CabinetPageResponseVo<CabinetHistoryResponseVo> responseVo = cabinetUtilService.convertToPageResponseVo(
                historyVOsPage,
                pageVo,
                request
        );

        // VO 목록을 DTO로 변환 (캐스팅 불필요)
        List<CabinetHistoryResponseDto> responseDto = responseVo.results().stream()
                .map(vo -> CabinetHistoryResponseDto.builder()
                        .building(vo.building())
                        .floor(vo.floor())
                        .section(vo.section())
                        .cabinetNumber(vo.cabinetNumber())
                        .startDate(vo.startDate())
                        .endDate(vo.endDate())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/status/search")
    @Operation(summary = "사물함 상태별 조회")
    public ResponseEntity<?> findCabinetsByStatus(
            @ModelAttribute CabinetSearchByStatusRequestDto requestDto,
            HttpServletRequest request
    ){
        // Pageable 생성
        Pageable pageable = PageRequest.of(
                Optional.ofNullable(requestDto.getPage()).orElse(0),
                Optional.ofNullable(requestDto.getPageSize()).orElse(12)
        );

        CabinetStatusVo statusVo = new CabinetStatusVo(requestDto.getStatus());

        Page<CabinetByStatusVo> page = cabinetService.findCabinetsByStatus(statusVo,pageable);

        List<CabinetByStatusResponseDto> cabinetByStatusResponseDtoList = page.getContent().stream()
                .map(vo ->CabinetByStatusResponseDto.builder()
                        .id(vo.id())
                        .building(vo.building())
                        .floor(vo.floor())
                        .se
                        .build()
                ).toList();
    }
}