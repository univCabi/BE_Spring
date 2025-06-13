package org.univcabi.univcabi.cabinet.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("position")
    private CabinetPositionDto positionDto;
    private String cabinetNumber;
    private CabinetStatus status;
    @JsonProperty("user")
    private CabinetUserDto cabinetUserDto;
    private String reason;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.M.d")
    private LocalDate rentalStartDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.M.d")
    private LocalDate overDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.M.d")
    private LocalDate brokenDate;
}
