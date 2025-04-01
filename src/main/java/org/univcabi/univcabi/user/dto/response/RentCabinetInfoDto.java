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
    public static RentCabinetInfoDto of(String building,
                                        Integer floor,
                                        Integer cabinetNumber,
                                        String status,
                                        LocalDateTime startDate,
                                        LocalDateTime endDate,
                                        Integer leftDate){
        return RentCabinetInfoDto.builder()
                .building(building)
                .floor(floor)
                .cabinetNumber(cabinetNumber)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .leftDate(leftDate)
                .build();
    }
}
