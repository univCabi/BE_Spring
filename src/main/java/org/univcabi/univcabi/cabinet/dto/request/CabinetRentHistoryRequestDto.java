package org.univcabi.univcabi.cabinet.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CabinetRentHistoryRequestDto {
    String studentNumber;
}
