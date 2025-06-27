package org.univcabi.univcabi.cabinet.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.vo.CabinetReturnDataVo;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CabinetReturnResultResponseDto {

    @JsonProperty("cabinets")
    private List<CabinetReturnDataResponseDto> dtoList;
    private String message;
    private List<String> errors;
}
