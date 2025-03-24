package org.univcabi.univcabi.cabinet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.entity.BuildingName;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetSearchResponseDto {
    private BuildingName building;
    private int floor;
    private String cabinetNumber;
}
