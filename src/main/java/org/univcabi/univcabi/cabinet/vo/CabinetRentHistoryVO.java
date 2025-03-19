package org.univcabi.univcabi.cabinet.vo;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class CabinetRentHistoryVO {
    String studentNumber;
}