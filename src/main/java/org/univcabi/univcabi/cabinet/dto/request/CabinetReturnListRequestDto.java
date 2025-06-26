package org.univcabi.univcabi.cabinet.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CabinetReturnListRequestDto {
    private List<Long> cabinetIds;
}
