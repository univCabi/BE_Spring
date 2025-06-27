package org.univcabi.univcabi.cabinet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CabinetLocationRequestDto {

    @NotBlank
    private String building;

    @NotNull
    private Integer floor;
}
