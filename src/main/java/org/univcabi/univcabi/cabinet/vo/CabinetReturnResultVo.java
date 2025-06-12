package org.univcabi.univcabi.cabinet.vo;

import java.util.List;

public record CabinetReturnResultVo(
        List<CabinetReturnDataVo> listVos,
        String message,
        List<String> errors
) {
}
