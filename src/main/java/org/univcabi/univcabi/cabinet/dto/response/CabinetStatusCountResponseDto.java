package org.univcabi.univcabi.cabinet.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.vo.CabinetStatusCountVo;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetStatusCountResponseDto {
    @JsonProperty("buildings")
    List<CabinetStatusCountVo> vos;
}
