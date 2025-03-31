package org.univcabi.univcabi.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserProfileResponseDto {
    private String name;
    private Boolean isVisible;
    private String affiliation;
    private String phoneNumber;
    private RentCabinetInfoDto rentCabinetInfoDto;
}