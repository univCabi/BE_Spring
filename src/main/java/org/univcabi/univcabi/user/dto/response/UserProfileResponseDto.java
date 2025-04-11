package org.univcabi.univcabi.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.univcabi.univcabi.user.vo.RentCabinetInfoVo;
import org.univcabi.univcabi.user.vo.UserProfileVo;

@Builder
@Getter
public class UserProfileResponseDto {
    private String name;
    private Boolean isVisible;
    private String affiliation;
    private String studentNumber;
    private String phoneNumber;
    private RentCabinetInfoDto rentCabinetInfo;

    // 정적 팩토리 메서드
    public static UserProfileResponseDto of(UserProfileVo vo){
        RentCabinetInfoVo infoVo = vo.rentCabinetInfoVo();

        return UserProfileResponseDto.builder()
                .name(vo.name())
                .isVisible(vo.isVisible())
                .affiliation(vo.affiliation())
                .studentNumber(vo.studentNumber())
                .phoneNumber(vo.phoneNumber())
                .rentCabinetInfo(
                        vo.rentCabinetInfoVo() == null ? null : RentCabinetInfoDto.of(
                                infoVo.building(),
                                infoVo.floor(),
                                infoVo.cabinetNumber(),
                                infoVo.status(),
                                infoVo.startDate(),
                                infoVo.endDate(),
                                infoVo.leftDate())
                )
                .build();
    }
}