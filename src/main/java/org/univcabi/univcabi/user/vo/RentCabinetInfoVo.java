package org.univcabi.univcabi.user.vo;


import java.time.LocalDate;
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
    // endDate 값이 null 일시에 default 로 현재로부터 2주 후가 endDate가 되도록 설정
    public int leftDate(){
        return (int) ChronoUnit.DAYS.between(LocalDateTime.now(),endDate == null? LocalDateTime.now().plusWeeks(2):endDate);
    }
}
