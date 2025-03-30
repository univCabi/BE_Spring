package org.univcabi.univcabi.cabinet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.univcabi.univcabi.cabinet.entity.BuildingName;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetHistoryResponseDto {
    private BuildingName building;
    private Integer floor;
    private String section;
    private String cabinetNumber;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

}
