package org.univcabi.univcabi.cabinet.vo;

import lombok.Builder;
import lombok.Value;

// 캐비닛 상세 조회 VO
@Value
@Builder
public class CabinetDetailVo {
    Long cabinetId;
}