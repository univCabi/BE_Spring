package org.univcabi.univcabi.cabinet.dto.response;

import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDate;

public class CabinetByStatusResponseDto {
    private Long id;
    private String building;
    private Integer floor;
    private CabinetPositionDto positionDto;
    private String cabinetNumber;
    private CabinetStatus status;
    private CabinetUserDto cabinetUserDto;
    private String reason;
    private LocalDate rentalStartDate;
    private LocalDate overDate;
}
