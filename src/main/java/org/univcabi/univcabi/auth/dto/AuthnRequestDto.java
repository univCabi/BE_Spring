package org.univcabi.univcabi.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
public class AuthnRequestDto {
    private String studentNumber;
    private String password;
}
