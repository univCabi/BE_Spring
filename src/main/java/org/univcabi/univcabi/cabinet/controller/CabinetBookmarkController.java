package org.univcabi.univcabi.cabinet.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.cabinet.dto.request.CabinetBookmarkRequestDto;
import org.univcabi.univcabi.cabinet.dto.response.CabinetBookmarkResponseDto;
import org.univcabi.univcabi.cabinet.service.CabinetBookmarkService;
import org.univcabi.univcabi.cabinet.service.CabinetService;
import org.univcabi.univcabi.cabinet.vo.CabinetBookmarkVo;

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
}
