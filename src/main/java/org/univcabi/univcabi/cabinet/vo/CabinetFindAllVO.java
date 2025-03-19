package org.univcabi.univcabi.cabinet.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CabinetFindAllVO {
    Integer page;
    Integer size;
}
