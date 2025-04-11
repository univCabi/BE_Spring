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
public class CabinetDetailResponseDto {
        private Integer floor;
        private String section;
        private BuildingName building;
        private String cabinetNumber;
        private CabinetStatus status;
        private Boolean isVisible;
        private String username;
        private Boolean isMine;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime expiredAt;
}
