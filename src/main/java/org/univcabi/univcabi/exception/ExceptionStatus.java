package org.univcabi.univcabi.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public enum ExceptionStatus {

    // GENERAL ERROR CODE
    GENERAL_BAD_REQUEST(HttpStatus.BAD_REQUEST, "서버에 잘못된 요청입니다."),
    GENERAL_REQUEST_INVALID_PARAMS(HttpStatus.BAD_REQUEST, "서버에 잘못된 요청입니다."),

    GENERAL_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 알 수 없는 오류가 발생했습니다"),
    GENERAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서버가 작동하지 않고 있습니다."),
    GENERAL_GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "서버에서 타임아웃이 발생했습니다");

    // 필드 선언은 열거형 상수 뒤에 위치
    private final int statusCode;
    private final String message;
    private final String error;

    // 생성자도 열거형 상수 뒤에 위치
    ExceptionStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
        this.error = status.getReasonPhrase();
    }
}

