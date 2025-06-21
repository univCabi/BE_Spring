package org.univcabi.univcabi.cabinet.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.univcabi.univcabi.cabinet.dto.request.CabinetBookmarkRequestDto;
import org.univcabi.univcabi.cabinet.dto.response.CabinetBookmarkResponseDto;
import org.univcabi.univcabi.cabinet.service.CabinetService;

@RestController
@RequestMapping("/cabinet/bookmark")
@Tag(name = "사물함 즐겨찾기 로직")
@RequiredArgsConstructor
public class CabinetBookmarkController {
    private final CabinetService cabinetService;

    @PostMapping("/add")
    public ResponseEntity<CabinetBookmarkResponseDto> addCabinetBookmarkByCabinetId(
            @RequestBody CabinetBookmarkRequestDto requestDto,
            Authentication authentication){

        return ResponseEntity.ok();
    }

    @PostMapping("/remove")
    public ResponseEntity<CabinetBookmarkResponseDto> removeCabinetBookmarkByCabinetId(
            @RequestBody CabinetBookmarkRequestDto requestDto,
            Authentication authentication){


        return ResponseEntity.ok();
    }

}
