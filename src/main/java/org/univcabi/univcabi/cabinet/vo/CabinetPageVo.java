package org.univcabi.univcabi.cabinet.vo;

public record CabinetPageVo(
        Integer page,
        Integer pageSize) {
    public static CabinetPageVo of(Integer page, Integer pageSize){
        return new CabinetPageVo(
                page,
                pageSize
        );
    }
}
