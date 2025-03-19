package org.univcabi.univcabi.cabinet.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.univcabi.univcabi.cabinet.dto.response.*;
import org.univcabi.univcabi.cabinet.repository.CabinetRepository;
import org.univcabi.univcabi.cabinet.vo.*;

@Service
public class CabinetService {

    private final CabinetRepository cabinetRepository;

    public CabinetService(CabinetRepository cabinetRepository) {
        this.cabinetRepository = cabinetRepository;
    }


    public Page<CabinetFindAllInfoResponseDto> findAllCabinetInfo(CabinetFindAllVo cabinetFindAllVO){

        // 페이지와 사이즈 정보를 Pageable 객체로 변환
        Pageable pageable = PageRequest.of(
                cabinetFindAllVO.getPage(),
                cabinetFindAllVO.getSize()
        );

        return cabinetRepository.findAllCabinetInfo(pageable);
    }

    public CabinetFindOneInfoResponseDto findOneCabinetInfo(CabinetDetailVo cabinetDetailVo) {
        return cabinetRepository.findAllCabinetInfo(cabinetDetailVo.getCabinetId());
    }

    public CabinetRentResponseDto rentCabinet(CabinetRentVo cabinetRentVo) {
        return cabinetRepository.rentCabinetByCabinetId(cabinetRentVo);
    }

    public CabinetReturnResponseDto returnCabinet(CabinetReturnVo cabinetReturnVo) {
       return cabinetRepository.returnCabinet(cabinetReturnVo);
    }

    public CabinetSearchResponseDto searchCabinetByKeyword(CabinetSearchVo cabinetSearchVo) {
        return cabinetRepository.searchCabinetByKeyword(cabinetSearchVo);
    }

    public CabinetSearchDetailResponseDto findCabinetRentHistory(CabinetRentHistoryVo cabinetRentHistoryVo) {
        return cabinetRepository.findCabinetRentHistory(cabinetRentHistoryVo);
    }
}
