package org.univcabi.univcabi.auth.dto;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
public class AuthnRequestDto {
    private String studentNumber;
    private String password;

    @JsonCreator
    public AuthnRequestDto(@JsonProperty("studentNumber") String studentNumber,
                           @JsonProperty("password") String password) {
        this.studentNumber = studentNumber;
        this.password = password;
    }
}
