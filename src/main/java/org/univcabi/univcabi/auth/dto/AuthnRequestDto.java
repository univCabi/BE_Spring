package org.univcabi.univcabi.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthnRequestDto {
    private String studentNumber;
    private String password;
}
