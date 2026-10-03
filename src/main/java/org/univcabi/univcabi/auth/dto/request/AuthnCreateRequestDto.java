package org.univcabi.univcabi.auth.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.univcabi.univcabi.auth.entity.AuthnRole;
import org.univcabi.univcabi.cabinet.entity.BuildingName;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthnCreateRequestDto {

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
