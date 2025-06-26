package org.univcabi.univcabi.cabinet.vo;

import java.util.List;

public record CabinetAdminChangeStatusResultVo(
        List<CabinetStatusInfoVo> cabinetStatusInfoVos,
        String message
) {
}
