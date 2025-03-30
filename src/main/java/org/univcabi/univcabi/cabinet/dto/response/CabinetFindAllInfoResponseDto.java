package org.univcabi.univcabi.cabinet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetFindAllInfoResponseDto {
    private long count;
    private String next;
    private String previous;
    private List<CabinetInfoResponseDto> results;

    // Static factory method if you still want to keep the 'of' method
    public static CabinetFindAllInfoResponseDto of(long count, String next, String previous, List<CabinetInfoResponseDto> results) {
        return CabinetFindAllInfoResponseDto.builder()
                .count(count)
                .next(next)
                .previous(previous)
                .results(results)
                .build();
    }
}