package org.univcabi.univcabi.cabinet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.vo.CabinetStatusInfoVo;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetAdminChangeStatusResponseDto {

    private List<CabinetStatusInfoVo> cabinets;

    private String message;
}
