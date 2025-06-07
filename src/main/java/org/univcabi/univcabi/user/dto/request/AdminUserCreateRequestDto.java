package org.univcabi.univcabi.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.cabinet.entity.BuildingName;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserCreateRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String affiliation;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String studentNumber;

    @NotBlank
    private String password;

    // 빌딩 정보는 nullable 이므로 null 값을 허용한다.
    private BuildingName buildingName;
    private Integer floor;
    private String section;
}
