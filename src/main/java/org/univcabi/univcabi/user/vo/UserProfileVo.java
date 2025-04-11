package org.univcabi.univcabi.user.vo;

public record UserProfileVo(
        String name,
        Boolean isVisible,
        String affiliation,
        String studentNumber,
        String phoneNumber,
        RentCabinetInfoVo rentCabinetInfoVo
) {
}
