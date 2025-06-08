package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;

import java.util.List;
import java.util.Optional;

public interface CabinetCustomRepository {

    Optional<Cabinet> findOneCabinetInfoByCabinetId(Long cabinetId);

    // 대여 처리
    Optional<Cabinet> rentCabinetByCabinetId(Long cabinetId, String studentNumber);

    // 반납 처리
    Optional<Cabinet> returnCabinetByCabinetId(Long cabinetId, String studentNumber);

    //대여 기록 조회
    Page<CabinetHistory> findCabinetHistoriesByStudentNumber(String studentNumber, Pageable pageable);

    //빌딩 과 층 정보로 사물함 정보 조회
    List<Cabinet> findCabinetByBuildingAndFloor(BuildingName buildingName, int floors);
}
