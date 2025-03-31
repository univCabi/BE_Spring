package org.univcabi.univcabi.user.vo;

public record UserProfileVo(
        String name,
        Boolean isVisible,
        String affiliation,
        String phoneNumber,
        RentCabinetInfoVo rentCabinetInfoVo
) {
}
