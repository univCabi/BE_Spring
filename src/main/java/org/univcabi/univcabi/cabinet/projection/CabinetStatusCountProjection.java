package org.univcabi.univcabi.cabinet.projection;

import org.univcabi.univcabi.cabinet.entity.BuildingName;

// 빌딩 이름별 사물함 상태 조회용 프로젝션 인터페이스
public interface CabinetStatusCountProjection {
    BuildingName getBuildingName();
    Long getTotalCount();
    Long getUsingCount();
    Long getOverdueCount();
    Long getBrokenCount();
    Long getAvailableCount();
}
