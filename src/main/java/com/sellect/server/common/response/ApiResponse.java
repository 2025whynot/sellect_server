package com.sellect.server.common.response;

public record ApiResponse<T>(
    T data,
    ErrorResponse error
) {

    public static <T> ApiResponse<T> OK(T data) {
        return new ApiResponse<>(data, null);
    }

    /*
     * Object - data: null로
     * Void - data 보이지 않게 되기에 의도와 다름
     * */
}


