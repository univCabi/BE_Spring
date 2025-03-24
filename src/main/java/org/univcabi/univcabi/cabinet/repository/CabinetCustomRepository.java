package org.univcabi.univcabi.cabinet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.univcabi.univcabi.cabinet.entity.Cabinet;
import org.univcabi.univcabi.cabinet.entity.CabinetHistory;
import org.univcabi.univcabi.cabinet.vo.CabinetHistoryVo;
import org.univcabi.univcabi.cabinet.vo.CabinetRentVo;
import org.univcabi.univcabi.cabinet.vo.CabinetReturnVo;

import java.util.Optional;

public interface CabinetCustomRepository {

    Optional<Cabinet> findOneCabinetInfoByCabinetId(Long cabinetId);

    // 대여 처리
    Optional<Cabinet> rentCabinetByCabinetId(CabinetRentVo requestVo);

    // 반납 처리
    Optional<Cabinet> returnCabinetByCabinetId(CabinetReturnVo requestVo);

    //대여 기록 조회
    Page<CabinetHistory> findCabinetHistoriesByStudentNumber(String studentNumber, Pageable pageable);

}
