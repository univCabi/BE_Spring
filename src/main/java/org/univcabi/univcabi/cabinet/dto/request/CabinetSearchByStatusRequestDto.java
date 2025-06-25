package org.univcabi.univcabi.cabinet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetSearchByStatusRequestDto {

    @NotBlank
    private CabinetStatus status;

    private Integer page;

    private Integer pageSize;

}
