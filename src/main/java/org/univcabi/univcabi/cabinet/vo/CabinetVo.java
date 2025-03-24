package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.BuildingName;

public record CabinetVo(
        // 단일 캐비닛 정보용 필드
        BuildingName buildingName,
        int floor,
        String cabinetNumber
) {
    // 용도별 편의 생성자 추가
    public CabinetVo() {
        this(null, 0 , null);
    }
}