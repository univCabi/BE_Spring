package org.univcabi.univcabi.cabinet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetPageResponseDto<T> {
    private Integer count;
    private String next;
    private String previous;
    private List<T> results;
}
