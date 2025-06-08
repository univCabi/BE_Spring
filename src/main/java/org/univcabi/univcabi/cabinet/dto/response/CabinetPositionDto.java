package org.univcabi.univcabi.cabinet.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CabinetPositionDto {
    private int x;
    private int y;
}
