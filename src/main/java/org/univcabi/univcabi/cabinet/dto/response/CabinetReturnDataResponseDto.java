package org.univcabi.univcabi.cabinet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CabinetReturnDataResponseDto {
    private Long id;
    private BuildingName buildingName;
    private String cabinetNumber;
    private CabinetStatus status;
    private String name;
}
