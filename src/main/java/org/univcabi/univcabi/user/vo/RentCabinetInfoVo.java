package org.univcabi.univcabi.user.vo;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record RentCabinetInfoVo (
         String building,
         Integer floor,
         Integer cabinetNumber,
         String status,
         LocalDateTime startDate,
         LocalDateTime endDate
) {
    public int leftDate(){
        return (int) ChronoUnit.DAYS.between(LocalDateTime.now(),endDate);
    }
}
