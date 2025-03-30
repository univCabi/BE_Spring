package org.univcabi.univcabi.cabinet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetPageResponseDto {
    private Integer count;
    private String next;
    private String previous;
}
