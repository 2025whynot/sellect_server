package com.sellect.server.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.List;

public record ApiResponse<T>(
        Boolean isSuccess,
        int status,
        String code,
        String message,
        List<ErrorResponse.FieldError> errors,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        T result
) {

    // 요청에 성공한 경우
    public static <T> ApiResponse<T> ok(T result) {
        return new ApiResponse<>(true, 200, "", "", null, result);
    }

    // 요청에 성공한 경우 - noContent 일 경우
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, 200, "", "", null, null);
    }

    //요청에 실패한 경우
    public static <T> ApiResponse<T> onFailure(ErrorResponse errorResponse, HttpStatus httpStatus) {
        return new ApiResponse<>(
                false,
                httpStatus.value(),
                errorResponse.getCode(),
                errorResponse.getMessage(),
                errorResponse.getErrors(),
                null);
    }
}

