package org.univcabi.univcabi.cabinet.vo;

import java.util.List;

public record CabinetPageResponseVo<T>(
        Integer count,       // 총 개수
        String next,         // 다음 페이지 URL (nullable)
        String previous,     // 이전 페이지 URL (nullable)
        List<T> results      // 결과 목록 (제네릭 타입)
) {}