package org.univcabi.univcabi.cabinet.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CabinetAdminChangeStatusRequestDto {

    @NotEmpty
    private List<Long> cabinetIds;

    @NotNull
    private CabinetStatus newStatus;

    private String reason;

    private String studentNumber;
}
