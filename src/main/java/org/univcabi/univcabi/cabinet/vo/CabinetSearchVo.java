package org.univcabi.univcabi.cabinet.vo;

import lombok.Builder;
import lombok.Value;

// 캐비닛 조회 관련 VO
@Value
@Builder
public class CabinetSearchVo {
    String keyword;
}