package org.univcabi.univcabi.cabinet.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.vo.*;
import org.univcabi.univcabi.cabinet.dto.request.*;
import org.univcabi.univcabi.user.entity.User;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CabinetService {

    private final CabinetRepository cabinetRepository;

    public CabinetService(CabinetRepository cabinetRepository) {
        this.cabinetRepository = cabinetRepository;
    }

    public CabinetPageResponseVo convertToPageResponseVo(
            Page<CabinetVo> cabinetPage, CabinetFindAllVo requestVo, HttpServletRequest request) {

        int currentPage = requestVo.page();
        int pageSize = requestVo.pageSize();

        String next = null;
        if (currentPage < cabinetPage.getTotalPages() - 1) {
            next = ServletUriComponentsBuilder.fromRequest(request)
                    .replaceQueryParam("page", currentPage + 1)
                    .replaceQueryParam("pageSize", pageSize)
                    .build()
                    .toUriString();
        }

        String previous = null;
        if (currentPage > 0) {
            previous = ServletUriComponentsBuilder.fromRequest(request)
                    .replaceQueryParam("page", currentPage - 1)
                    .replaceQueryParam("pageSize", pageSize)
                    .build()
                    .toUriString();
        }

        return new CabinetPageResponseVo(
                Math.toIntExact(cabinetPage.getTotalElements()),
                next,
                previous,
                cabinetPage.getContent()
        );
    }

    private CabinetVo convertToCabinetVo(Cabinet cabinet) {
        return new CabinetVo(
                cabinet.getBuildingId().getName().toString(), // BuildingName enum을 String으로 변환
                Integer.parseInt(cabinet.getBuildingId().getFloor()), // floor를 Integer로 변환 (저장 형식에 따라 조정 필요)
                cabinet.getCabinetNumber()
        );
    }

    public Page<CabinetVo> findAllCabinetInfo(CabinetFindAllVo requestVo) {
        Pageable pageable = PageRequest.of(
                requestVo.page(),
                requestVo.pageSize()
        );

        Page<Cabinet> cabinetPage = cabinetRepository.findAllCabinetInfo(pageable);

        // 조회 결과가 없는 경우 빈 페이지 반환
        if (cabinetPage == null) {
            return Page.empty(pageable);
        }

        // 엔티티 페이지를 VO 페이지로 변환
        return cabinetPage.map(this::convertToCabinetVo);
    }

    public CabinetDetailVo findOneCabinetInfo(CabinetFindOneVo requestVo) {
        Optional<Cabinet> cabinetOptional = cabinetRepository.findOneCabinetInfoByCabinetId(requestVo.cabinetId());

        if (cabinetOptional.isEmpty()) {
            throw new RuntimeException("캐비닛을 찾을 수 없습니다.");
        }

        Cabinet cabinet = cabinetOptional.get();
        Building building = cabinet.getBuildingId();
        User user = cabinet.getUserId();

        // Record 타입으로 가정
        CabinetDetailVo cabinetDetailVo = new CabinetDetailVo(
                building.getFloor(),
                cabinet.getCabinetNumber().substring(0, 1),
                building.getName(),
                cabinet.getCabinetNumber(),
                cabinet.getStatus(),
                cabinet.getPayable(),
                user.getName(),
                user.getId().equals(requestVo.userId()),
                cabinet.getUpdatedAt()
        );

        return cabinetDetailVo;
    }

    public CabinetVO rentCabinet(CabinetRentVo requestVo) {
        Cabinet cabinet = cabinetRepository.rentCabinetByCabinetId(requestVo);
        if (cabinet == null) {
            throw new RuntimeException("캐비닛 대여에 실패했습니다.");
        }
        return convertToCabinetVO(cabinet);
    }

    public CabinetVO returnCabinet(CabinetReturnVo requestVo) {
        // CabinetReturnRequestDto를 직접 생성
        CabinetReturnRequestDto requestDto = new CabinetReturnRequestDto();
        // requestDto.setCabinetId(requestVo.cabinetId()); // setter 또는 생성자로 설정

        List<Cabinet> cabinets = cabinetRepository.findCabinetBy(requestDto)
                .orElse(Collections.emptyList());

        if (cabinets.isEmpty()) {
            throw new RuntimeException("반납할 캐비닛을 찾을 수 없습니다.");
        }

        Cabinet cabinet = cabinetRepository.returnCabinet(requestVo);
        if (cabinet == null) {
            throw new RuntimeException("캐비닛 반납에 실패했습니다.");
        }
        return convertToCabinetVO(cabinet);
    }

    public List<CabinetVO> searchCabinetByKeyword(CabinetSearchVo requestVo) {
        Pageable pageable = PageRequest.of(0, 5);
        List<Cabinet> cabinets = cabinetRepository.searchCabinetByKeyword(requestVo.keyword(), pageable);

        return cabinets.stream()
                .map(this::convertToCabinetVO)
                .collect(Collectors.toList());
    }

    public List<CabinetRentHistoryVO> findCabinetRentHistory(CabinetRentHistoryVo requestVo) {
        // DTO 생성 및 변환
        CabinetRentHistoryRequestDto requestDto = new CabinetRentHistoryRequestDto();
        // requestDto.setStudentNumber(requestVo.studentNumber()); // setter 또는 생성자로 설정

        List<CabinetRentHistory> histories = cabinetRepository.findRentHistoryByStudentNumber(requestVo.studentNumber());

        if (histories == null) {
            return Collections.emptyList();
        }

        return histories.stream()
                .map(this::convertToRentHistoryVO)
                .collect(Collectors.toList());
    }

    public List<CabinetVO> searchDetailByKeyword(CabinetSearchVo requestVo) {
        // DTO 생성 및 변환
        CabinetSearchDetailRequestDto requestDto = new CabinetSearchDetailRequestDto();
        // requestDto.setKeyword(requestVo.keyword()); // setter 또는 생성자로 설정

        List<Cabinet> cabinets = cabinetRepository.findAllCabinetInfo(requestDto)
                .orElse(Collections.emptyList());

        return cabinets.stream()
                .map(this::convertToCabinetVO)
                .collect(Collectors.toList());
    }

    // 엔티티를 VO로 변환하는 메서드들
    private CabinetVo convertToCabinetVO(Cabinet cabinet) {
        return new CabinetVo(
                cabinet.getId(),
                cabinet.getCabinetNumber(),
                cabinet.getStatus(),
                cabinet.getBuildingId().getName(),
                cabinet.getPayable()
        );
    }


    private CabinetRentHistoryVO convertToRentHistoryVO(CabinetRentHistory history) {
        return new CabinetRentHistoryVO(
                history.getId(),
                history.getCabinet().getId(),
                history.getCabinet().getCabinetNumber(),
                history.getStartDate(),
                history.getEndDate(),
                history.getStatus()
        );
    }
}