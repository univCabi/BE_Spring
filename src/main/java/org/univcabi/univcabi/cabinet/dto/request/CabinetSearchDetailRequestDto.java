package org.univcabi.univcabi.cabinet.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CabinetSearchDetailRequestDto {
    private String keyword;
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer pageSize = 12;
}
