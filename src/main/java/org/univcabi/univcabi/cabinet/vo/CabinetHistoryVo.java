package org.univcabi.univcabi.cabinet.vo;

public record CabinetHistoryVo(
        String studentNumber,
        Integer page,
        Integer pageSize) {
    public CabinetHistoryVo {
        // Default values if null
        if (page == null) page = 0;
        if (pageSize == null) pageSize = 12;
    }
}

