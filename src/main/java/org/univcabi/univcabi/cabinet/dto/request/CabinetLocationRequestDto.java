package org.univcabi.univcabi.cabinet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CabinetLocationRequestDto {

    @NotBlank
    private String building;

    @NotBlank
    private Integer floors;
}
