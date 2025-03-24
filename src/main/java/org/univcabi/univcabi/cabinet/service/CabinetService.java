package org.univcabi.univcabi.cabinet.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.univcabi.univcabi.cabinet.entity.Building;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.vo.*;
import org.univcabi.univcabi.user.entity.User;

import java.util.ArrayList;
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

    public <T> CabinetPageResponseVo<T> convertToPageResponseVo(
            Page<T> page, CabinetPageVo requestVo, HttpServletRequest request) {

        int currentPage = requestVo.page();
        int pageSize = requestVo.pageSize();

        String next = null;
        if (currentPage < page.getTotalPages() && !page.isEmpty()) {
            next = ServletUriComponentsBuilder.fromRequest(request)
                    .replaceQueryParam("page", currentPage + 1)
                    .replaceQueryParam("pageSize", pageSize)
                    .build()
                    .toUriString();
        }

        String previous = null;
        if (currentPage > 1) {
            previous = ServletUriComponentsBuilder.fromRequest(request)
                    .replaceQueryParam("page", currentPage - 1)
                    .replaceQueryParam("pageSize", pageSize)
                    .build()
                    .toUriString();
        }

        return new CabinetPageResponseVo<>(
                Math.toIntExact(page.getTotalElements()),
                next,
                previous,
                page.getContent()
        );
    }

    public Page<CabinetVo> findAllCabinetInfo(CabinetPageVo requestVo) {
        Pageable pageable = PageRequest.of(
                requestVo.page(),
                requestVo.pageSize()
        );

        Page<Cabinet> cabinetPage = cabinetRepository.findAllCabinetInfo(pageable);

        // Page<Cabinet>을 Page<CabinetVo>로 변환
        return cabinetPage.map(cabinet -> new CabinetVo(
                cabinet.getBuildingId().getName(),
                cabinet.getBuildingId().getFloor(),
                cabinet.getCabinetNumber()
        ));
    }

    public CabinetDetailVo findOneCabinetInfo(CabinetFindOneVo requestVo) {
        // 1. 캐비닛 조회
        Optional<Cabinet> cabinetOptional = cabinetRepository.findOneCabinetInfoByCabinetId(requestVo.cabinetId());

        if (cabinetOptional.isEmpty()) {
            throw new RuntimeException("캐비닛을 찾을 수 없습니다.");
        }

        Cabinet cabinet = cabinetOptional.get();
        Building building = cabinet.getBuildingId();
        User cabinetOwner = cabinet.getUserId();

        // 2. studentNumber로 요청자 확인 (isMine 여부 판단)
        boolean isMine = checkIsMine(requestVo.studentNumber(), cabinetOwner);

        //TODO: 왜 Boolean 이거여야 하는지 알아보기
        Boolean isVisible = (cabinetOwner != null) ? cabinetOwner.getIsVisible() : false;

        // 3. Entity를 VO로 변환
        return new CabinetDetailVo(
                building.getFloor(),
                cabinet.getCabinetNumber().substring(0, 1), // section
                building.getName(),
                cabinet.getCabinetNumber(),
                cabinet.getStatus(),
                isVisible,
                cabinetOwner != null ? cabinetOwner.getName() : null,
                isMine,
                cabinet.getUpdatedAt() // 만료일
        );
    }

    public CabinetDetailVo rentCabinet(CabinetRentVo requestVo) {
        Optional<Cabinet> cabinetOptional = cabinetRepository.rentCabinetByCabinetId(requestVo);

        if (cabinetOptional.isEmpty()) {
            throw new RuntimeException("캐비닛을 찾을 수 없습니다.");
        }
        Cabinet cabinet = cabinetOptional.get();
        Building building = cabinet.getBuildingId();
        User cabinetOwner = cabinet.getUserId();

        // 2. studentNumber로 요청자 확인 (isMine 여부 판단)
        boolean isMine = checkIsMine(requestVo.studentNumber(), cabinetOwner);

        Boolean isVisible = (cabinetOwner != null) ? cabinetOwner.getIsVisible() : false;

        return new CabinetDetailVo(
                building.getFloor(),
                cabinet.getCabinetNumber().substring(0, 1), // section
                building.getName(),
                cabinet.getCabinetNumber(),
                cabinet.getStatus(),
                isVisible,
                cabinetOwner != null ? cabinetOwner.getName() : null,
                isMine,
                cabinet.getUpdatedAt() // 만료일
        );

    }

    public CabinetDetailVo returnCabinet(CabinetReturnVo requestVo) {
        Optional<Cabinet> cabinetOptional = cabinetRepository.returnCabinetByCabinetId(requestVo);

        if (cabinetOptional.isEmpty()) {
            throw new RuntimeException("캐비닛을 찾을 수 없습니다.");
        }
        Cabinet cabinet = cabinetOptional.get();
        Building building = cabinet.getBuildingId();
        User cabinetOwner = cabinet.getUserId();

        // 2. studentNumber로 요청자 확인 (isMine 여부 판단)
        boolean isMine = checkIsMine(requestVo.studentNumber(), cabinetOwner);

        return new CabinetDetailVo(
                building.getFloor(),
                cabinet.getCabinetNumber().substring(0, 1), // section
                building.getName(),
                cabinet.getCabinetNumber(),
                cabinet.getStatus(),
                cabinet.getUserId().getIsVisible(),
                cabinetOwner != null ? cabinetOwner.getName() : null,
                isMine,
                cabinet.getUpdatedAt() // 만료일
        );
    }

    public List<CabinetVo> searchCabinetByKeyword(CabinetSearchVo requestVo) {
        // 키워드로 캐비닛 검색
        Page<Cabinet> cabinetPage = cabinetRepository.searchCabinetsByKeyword(
                requestVo.keyword(),
                PageRequest.of(0, 5) // 기본 페이지네이션 설정
        );

        // 조회 결과가 없는 경우 빈 리스트 반환
        if (cabinetPage == null || cabinetPage.isEmpty()) {
            return new ArrayList<>();
        }

        // 현재 CabinetVo 구조에 맞게 변환
        return cabinetPage.getContent().stream()
                .map(cabinet -> new CabinetVo(
                        cabinet.getBuildingId().getName(),  // buildingName
                        cabinet.getBuildingId().getFloor(), // floor
                        cabinet.getCabinetNumber()          // cabinetNumber
                ))
                .collect(Collectors.toList());
    }

    public Page<CabinetVo> searchDetailByKeyword(CabinetSearchDetailVo requestVo) {
        // 페이징 정보 생성
        Pageable pageable = PageRequest.of(
                requestVo.page() - 1,  // 0부터 시작하므로 1 빼기
                requestVo.pageSize()
        );

        // 키워드 검색 쿼리 실행
        Page<Cabinet> cabinetPage = cabinetRepository.findAllCabinetInfoByKeyword(
                requestVo.keyword(),
                pageable
        );

        // Page<Cabinet>을 Page<CabinetVo>로 변환
        return cabinetPage.map(cabinet -> new CabinetVo(
                cabinet.getBuildingId().getName(),
                cabinet.getBuildingId().getFloor(),
                cabinet.getCabinetNumber()
        ));
    }

    public Page<CabinetHistoryResponseVo> findCabinetRentHistory(CabinetHistoryVo requestVo) {
        // Create pageable with proper pagination
        Pageable pageable = PageRequest.of(
                requestVo.page() - 1,  // Convert to 0-based indexing
                requestVo.pageSize()
        );

        // Fetch histories with pagination
        Page<CabinetHistory> historyPage = cabinetRepository
                .findCabinetHistoriesByStudentNumber(requestVo.studentNumber(), pageable);

        // Map entities to response VOs
        return historyPage.map(history -> {
            Cabinet cabinet = history.getCabinet();
            Building building = cabinet.getBuildingId();

            return new CabinetHistoryResponseVo(
                    building.getName(),
                    building.getFloor(),
                    building.getSection(),
                    cabinet.getCabinetNumber(),
                    history.getCreatedAt(),
                    history.getEndedAt()
            );
        });
    }


    private boolean checkIsMine(String studentNumber, User cabinetOwner) {
        if (studentNumber != null && cabinetOwner != null) {
            //TODO: 추가예정
//            // 학번으로 사용자 찾기
//            Optional<User> requestUser = userRepository.findByStudentNumber(requestVo.studentNumber());
//
//            if (requestUser.isPresent()) {
//                return requestUser.get().getId().equals(cabinetOwner.getId());
//            }
        }
        return false;
    }
}