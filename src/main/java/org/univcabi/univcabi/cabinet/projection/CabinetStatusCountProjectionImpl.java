package org.univcabi.univcabi.cabinet.projection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.univcabi.univcabi.cabinet.entity.BuildingName;

@Value // 불변 객체
@Builder
@AllArgsConstructor
public class CabinetStatusCountProjectionImpl implements CabinetStatusCountProjection{
    BuildingName buildingName;
    Long totalCount;
    Long usingCount;
    Long overdueCount;
    Long brokenCount;
    Long availableCount;
}
