package org.univcabi.univcabi.cabinet.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CabinetFindAllVo {
    Integer page;
    Integer size;
}
