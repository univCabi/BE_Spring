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
public class CabinetInfoResponseDto {
    private BuildingName building;
    private String cabinetNumber;
    private Integer floor;
}