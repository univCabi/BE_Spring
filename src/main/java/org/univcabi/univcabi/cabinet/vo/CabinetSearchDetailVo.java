package org.univcabi.univcabi.cabinet.vo;

public record CabinetSearchDetailVo(String keyword, Integer page, Integer pageSize) {
    public CabinetSearchDetailVo {
        // Default values if null
        if (page == null) page = 0;
        if (pageSize == null) pageSize = 12;
    }
}
