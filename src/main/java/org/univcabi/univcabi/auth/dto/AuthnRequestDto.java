package org.univcabi.univcabi.auth.dto;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.univcabi.univcabi.auth.entity.AuthnRole;


@Getter
@Builder
public class AuthnRequestDto {
    private String studentNumber;
    private String password;
    private AuthnRole role;

    @JsonCreator
    public AuthnRequestDto(@JsonProperty("studentNumber") String studentNumber,
                           @JsonProperty("password") String password,
                           @JsonProperty("role") AuthnRole role) {
        this.studentNumber = studentNumber;
        this.password = password;
        this.role = role;
    }
}
