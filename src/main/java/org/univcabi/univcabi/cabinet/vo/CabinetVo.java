package org.univcabi.univcabi.cabinet.vo;

public record CabinetVo(
        // 단일 캐비닛 정보용 필드
        String buildingName,
        int floor,
        String cabinetNumber
) {
    // 용도별 편의 생성자 추가
    public CabinetVo() {
        this(null, 0 , null);
    }
}