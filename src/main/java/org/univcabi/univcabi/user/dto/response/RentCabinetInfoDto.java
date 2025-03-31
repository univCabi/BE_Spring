package org.univcabi.univcabi.user.dto.response;


import lombok.Builder;
import lombok.Getter;

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
}
