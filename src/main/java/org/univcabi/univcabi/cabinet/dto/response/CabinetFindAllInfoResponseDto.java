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
public class CabinetFindAllInfoResponseDto extends CabinetPageResponseDto {
    // 원하는 타입의 result 필드 정의
    private List<CabinetInfoResponseDto> result;

    // 정적 팩토리 메서드 추가
    public static CabinetFindAllInfoResponseDto of(Integer count, String next, String previous,
                                                   List<CabinetInfoResponseDto> result) {
        CabinetFindAllInfoResponseDto dto = new CabinetFindAllInfoResponseDto();
        dto.setCount(count);
        dto.setNext(next);
        dto.setPrevious(previous);
        dto.setResult(result);
        return dto;
    }
}