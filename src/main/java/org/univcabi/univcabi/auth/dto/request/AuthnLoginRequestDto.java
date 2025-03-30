package org.univcabi.univcabi.auth.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthnLoginRequestDto {
    private String studentNumber;
    private String password;
}
