package org.univcabi.univcabi.cabinet.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.util.List;

public class CabinetAdminChangeStatusRequestDto {

    @NotEmpty
    private List<Long> cabinetIds;

    @NotNull
    private CabinetStatus newStatus;

    private String reason;

    private String studentNumber;
}
