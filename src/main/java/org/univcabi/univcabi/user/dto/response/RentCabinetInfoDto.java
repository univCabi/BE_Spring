package org.univcabi.univcabi.user.dto.response;


import lombok.Builder;
import lombok.Getter;
import org.univcabi.univcabi.user.vo.RentCabinetInfoVo;

import java.time.LocalDateTime;

@Builder
@Getter
// 예를 CabinetRentInfoDto 로 이름 바꾸고 Cabinet 폴더에서 다뤄야할지 고민
public class RentCabinetInfoDto {
    private String building;
    private Integer floor;
    private Integer cabinetNumber;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer leftDate;

    // UserProfileResponseDto 의 정적 펙토리 메서드에 포함 되어 있음
    public static RentCabinetInfoDto of(RentCabinetInfoVo vo){
        return RentCabinetInfoDto.builder()
                .building(vo.building())
                .floor(vo.floor())
                .cabinetNumber(vo.cabinetNumber())
                .startDate(vo.startDate())
                .status(vo.status())
                .startDate(vo.startDate())
                .endDate(vo.endDate())
                .leftDate(vo.leftDate())
                .build();
    }
}
