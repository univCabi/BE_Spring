package org.univcabi.univcabi.cabinet.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDateTime;

public record CabinetDetailVo(
        Integer floor,
        String section,
        BuildingName building,
        String cabinetNumber,
        CabinetStatus status,
        Boolean isVisible,
        String username,
        Boolean isMine,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // 날짜 형식 명시적 지정
        LocalDateTime expiredAt
)
{}