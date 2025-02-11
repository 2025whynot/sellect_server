package com.sellect.server.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GlobalException.class)
    protected ApiResponse<?> BaseError(GlobalException e, HttpServletRequest request) {
        log.error("errorStatus: {}, url: {}, message: {}", e.getStatus(), request.getRequestURI(), e.getMessage());

        return new ApiResponse<>(e.getStatus());
    }

    //    runtime error
    @ExceptionHandler(RuntimeException.class)
    protected ApiResponse<?> RuntimeError(RuntimeException e, HttpServletRequest request) {
        log.error("url: {}, message: {}", request.getRequestURI(), e.getMessage());

        return new ApiResponse<>(ResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> processVaildationError(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.error("url: {}, message: {}", request.getRequestURI(), e.getMessage());
        BindingResult bindingResult = e.getBindingResult();

        StringBuilder builder = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            builder.append(fieldError.getDefaultMessage());
            builder.append("\n ");
        }
        return new ApiResponse<>(e, builder.toString());
    }

}
