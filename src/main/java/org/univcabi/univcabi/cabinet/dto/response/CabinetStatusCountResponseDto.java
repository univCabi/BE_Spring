package org.univcabi.univcabi.cabinet.dto.response;

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
    List<CabinetStatusCountVo> vos;
}
