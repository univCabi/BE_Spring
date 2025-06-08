package org.univcabi.univcabi.cabinet.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CabinetUserDto {
    private String name;
    private String studentNumber;
}
