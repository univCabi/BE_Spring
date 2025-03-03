package org.univcabi.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthnRequestDto {
    private String studentNumber;
    private String password;
}
