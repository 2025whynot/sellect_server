package com.sellect.server.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        Boolean isSuccess,
        String code,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        T result
) {
    /**
     * 필요값 : 성공여부, 메시지, 에러코드, 결과값
     */

    // 요청에 성공한 경우 -> return 객체가 필요한 경우
    public static <T> ApiResponse<T> OK(T result) {
        return new ApiResponse<>(true, "200", null, result);
    }

    // 요청 실패한 경우
    public ApiResponse(ResponseStatus status) {
        this(false, status.getCode(), status.getMessage(), null);
    }

    // 요청 실패한 경우 @RuntimeError
    public ApiResponse(ResponseStatus status, String message) {
        this(false, status.getCode(), message, null);
    }

    //요청에 실패한 경우 @Vaild annotantion error
    public ApiResponse(Exception e, String message) {
        this(false, "ERROR3000", message, null);
    }

}

