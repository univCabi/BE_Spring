package org.univcabi.univcabi.cabinet.dto.response;

import lombok.*;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetByStatusResponseDto {
    private Long id;
    private BuildingName building;
    private Integer floor;
    private String section;
    private CabinetPositionDto positionDto;
    private String cabinetNumber;
    private CabinetStatus status;
    private CabinetUserDto cabinetUserDto;
    private String reason;
    private LocalDate rentalStartDate;
    private LocalDate overDate;
    private LocalDate brokenDate;
}
