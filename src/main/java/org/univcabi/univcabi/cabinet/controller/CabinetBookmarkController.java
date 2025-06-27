package org.univcabi.univcabi.cabinet.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.cabinet.dto.request.CabinetBookmarkRequestDto;
import org.univcabi.univcabi.cabinet.dto.response.CabinetBookmarkListResponseDto;
import org.univcabi.univcabi.cabinet.dto.response.CabinetBookmarkResponseDto;
import org.univcabi.univcabi.cabinet.service.CabinetBookmarkService;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkAuthVo;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkListVo;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkVo;

import java.util.List;

@RestController
@RequestMapping("/cabinet/bookmark")
@Tag(name = "사물함 즐겨찾기 로직")
@RequiredArgsConstructor
public class CabinetBookmarkController {
    private final CabinetService cabinetService;
    private final CabinetBookmarkService cabinetBookmarkService;

    @PostMapping("/add")
    public ResponseEntity<CabinetBookmarkResponseDto> addCabinetBookmarkByCabinetId(
            @RequestBody @Valid CabinetBookmarkRequestDto requestDto,
            Authentication authentication){

        String studentNumber = authentication.getName();
        CabinetBookmarkVo requestVo = new CabinetBookmarkVo(requestDto.getCabinetId(),studentNumber);

        cabinetBookmarkService.addBookmarkByCabinetId(requestVo);

        return ResponseEntity.ok(CabinetBookmarkResponseDto.builder()
                .isBookmark(true)
                .build());
    }

    @PostMapping("/remove")
    public ResponseEntity<CabinetBookmarkResponseDto> removeCabinetBookmarkByCabinetId(
            @RequestBody @Valid CabinetBookmarkRequestDto requestDto,
            Authentication authentication){

        String studentNumber = authentication.getName();
        CabinetBookmarkVo requestVo = new CabinetBookmarkVo(requestDto.getCabinetId(),studentNumber);

        cabinetBookmarkService.removeBookmarkByCabinetId(requestVo);

        return ResponseEntity.ok(CabinetBookmarkResponseDto.builder()
                .isBookmark(false)
                .build());
    }

    @GetMapping("/list")
    public ResponseEntity<List<CabinetBookmarkListResponseDto>> getCabinetBookmarkList(
      Authentication authentication
    ) {
        String studentNumber = authentication.getName();
        CabinetBookmarkAuthVo requestVo = new CabinetBookmarkAuthVo(studentNumber);

        List<CabinetBookmarkListVo> responseVo = cabinetBookmarkService.getBookmarkList(requestVo);

        List<CabinetBookmarkListResponseDto> responseDtoList = responseVo.stream()
                .map(vo -> {
                    return CabinetBookmarkListResponseDto.builder()
                            .cabinetId(vo.cabinetId())
                            .cabinetNumber(vo.cabinetNumber())
                            .floor(vo.floor())
                            .section(vo.section())
                            .status(vo.status())
                            .building(vo.building())
                            .createdAt(vo.createdAt())
                            .build();
                })
                .toList();

        return ResponseEntity.ok(responseDtoList);
    }
}
