package org.univcabi.univcabi.cabinet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//TODO: 추후에 어노테이션 변경
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetFindOneInfoRequestDto {
    private Long cabinetId;

}