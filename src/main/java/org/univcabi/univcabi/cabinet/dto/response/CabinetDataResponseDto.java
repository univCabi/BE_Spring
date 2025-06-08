package org.univcabi.univcabi.cabinet.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;
import org.univcabi.univcabi.cabinet.vo.CabinetDataVo;

@Builder
@Getter
public class CabinetDataResponseDto {

    private Long id;
    private Integer cabinetNumber;
    private Integer cabinetXPos;
    private Integer cabinetYPos;
    private CabinetStatus status;

    private String isVisible;
    private String username;
    private Boolean isMine;
    private Boolean isRentAvailable;
    private Boolean isFree;

    public static CabinetDataResponseDto of(CabinetDataVo vo) {
        return CabinetDataResponseDto.builder()
                .id(vo.id())
                .cabinetNumber(vo.cabinetNumber())
                .cabinetXPos(vo.cabinetXPos())
                .cabinetYPos(vo.cabinetYPos())
                .status(vo.status())
                .isVisible(String.valueOf(vo.isVisible()))
                .username(vo.username())
                .isMine(vo.isMine())
                .isRentAvailable(vo.isRentAvailable())
                .isFree(vo.isFree())
                .build();
    }
}
