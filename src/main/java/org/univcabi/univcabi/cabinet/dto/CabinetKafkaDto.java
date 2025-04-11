package org.univcabi.univcabi.cabinet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDateTime;

public class CabinetKafkaDto {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CabinetRentMessage(
            Long cabinetId,
            String studentNumber,
            LocalDateTime requestTime
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CabinetReturnMessage(
            Long cabinetId,
            String studentNumber,
            LocalDateTime requestTime
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CabinetStatusUpdateMessage(
            Long cabinetId,
            CabinetStatus status,
            String studentNumber,
            LocalDateTime updateTime,
            boolean success,
            String errorMessage
    ) {}
}