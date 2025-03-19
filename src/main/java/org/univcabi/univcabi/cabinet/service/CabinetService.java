package org.univcabi.univcabi.cabinet.service;

import org.springframework.stereotype.Service;
import org.univcabi.univcabi.cabinet.vo.CabinetSearchVO;

@Service
public class CabinetService {

    private final CabinetRepository;

    public CabinetService(CabinetRepository cabinetRepository) {
        this.cabinetRepository = cabinetRepository;
    }


    public void findAllCabinetInfo(CabinetSearchVO cabinetSearchVO){
        cabinetRepository.findAllCabinetInfo(cabinetSearchVO);
    }

    public void findOneCabinetInfo(CabinetFindOneInfoRequestDto cabinetFindOneInfoRequestDto) {
        cabinetRepository.findOneCabinetInfo(cabinetFindOneInfoRequestDto);

    }

    public void rentCabinet(CabinetRentRequestDto cabinetRentRequestDto) {
        cabinetRepository.rentCabinet(cabinetRentRequestDto);
    }

    public void returnCabinet(CabinetReturnRequestDto cabinetReturnRequestDto) {
        cabinetRepository.returnCabinet(cabinetReturnRequestDto);
    }

    public void searchCabinetByKeyword(CabinetSearchDetailRequestDto cabinetSearchDetailRequestDto) {
        cabinetRepository.searchCabinetByKeyword(cabinetSearchDetailRequestDto);
    }

    public void findCabinetRentHistory(CabinetRentHistoryRequestDto cabinetRentHistoryRequestDto) {
        cabinetRepository.findCabinetRentHistory(cabinetRentHistoryRequestDto);
    }
}
