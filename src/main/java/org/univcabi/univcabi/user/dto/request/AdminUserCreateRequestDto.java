package org.univcabi.univcabi.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


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

    @NotBlank
    private String buildingName;

    @NotNull
    private Integer floor;

    @NotBlank
    private String section;
}
