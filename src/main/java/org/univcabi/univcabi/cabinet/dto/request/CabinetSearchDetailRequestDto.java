package org.univcabi.univcabi.cabinet.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CabinetSearchDetailRequestDto {
    private String keyword;
}
