package org.univcabi.univcabi.cabinet.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.entity.BuildingName;
import org.univcabi.univcabi.cabinet.entity.CabinetStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetBookmarkListResponseDto {
    private Long cabinetId;
    private BuildingName building;
    private Integer floor;
    private String section;
    private String cabinetNumber;
    private CabinetStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // 날짜 형식 명시적 지정
    private LocalDateTime createdAt;
}
