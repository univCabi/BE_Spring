package org.univcabi.univcabi.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.univcabi.univcabi.user.vo.UserProfileVo;

@Builder
@Getter
public class UserProfileResponseDto {
    private String name;
    private Boolean isVisible;
    private String affiliation;
    private String studentNumber;
    private String phoneNumber;
    private RentCabinetInfoDto rentCabinetInfoDto;

    // 정적 팩토리 메서드
    public static UserProfileResponseDto of(UserProfileVo vo){
        return UserProfileResponseDto.builder()
                .name(vo.name())
                .isVisible(vo.isVisible())
                .affiliation(vo.affiliation())
                .studentNumber(vo.studentNumber())
                .phoneNumber(vo.phoneNumber())
                .rentCabinetInfoDto(
                        vo.rentCabinetInfoVo() == null ? null : RentCabinetInfoDto.of(vo.rentCabinetInfoVo())
                )
                .build();
    }
}