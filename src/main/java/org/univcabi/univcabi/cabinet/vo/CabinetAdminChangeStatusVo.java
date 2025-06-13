package org.univcabi.univcabi.cabinet.vo;

import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.util.List;

public record CabinetAdminChangeStatusVo(
        List<Long> cabinetIds,
        CabinetStatus newStatus,
        String reason,
        String studentNumber
) {
}
