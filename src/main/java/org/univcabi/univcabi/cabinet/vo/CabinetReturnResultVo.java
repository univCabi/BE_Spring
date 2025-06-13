package org.univcabi.univcabi.cabinet.vo;

import java.util.List;

// /cabinet/admin/return api 에 사용되는 vo
public record CabinetReturnResultVo(
        List<CabinetReturnDataVo> listVos,
        String message,
        List<String> errors
) {
}
