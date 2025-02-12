package com.sellect.server.common.exception.handler;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.util.ErrorCode;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.BindException;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * javax.validation.Valid or @Validated 으로 binding error 발생시 발생한다.
	 * HttpMessageConverter 에서 등록한 HttpMessageConverter binding 못할 경우 발생
	 * 주로 @RequestBody, @RequestPart 어노테이션에서 발생
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ApiResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.error("handleMethodArgumentNotValidException {}", e.getMessage());
		final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
		return ApiResponse.onFailure(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * @ModelAttribute 으로 binding error 발생시 BindException 발생한다.
	 */
	@ExceptionHandler(BindException.class)
	protected ApiResponse handleBindException(BindException e) {
		log.error("handleBindException {}", e.getMessage());
		final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE);
		return ApiResponse.onFailure(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * @PathVariable 에서 validation을 할 때 binding error가 발생하는 경우
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	protected ApiResponse handleConstraintViolationException(ConstraintViolationException e) {
		log.error("handleConstraintViolationException {}", e.getMessage());
		Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
		final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, constraintViolations);
		return ApiResponse.onFailure(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * 주로 @RequestParam에서 enum으로 binding 못할 경우 발생
	 * @PathVariable에서 string -> int/long 등의 숫자 타입 binding 못할 경우
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	protected ApiResponse handleMethodArgumentTypeMismatchException(
			MethodArgumentTypeMismatchException e) {
		log.error("handleMethodArgumentTypeMismatchException {}", e.getMessage());
		final ErrorResponse response = ErrorResponse.of(e);
		return ApiResponse.onFailure(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * body to dto 과정에서 enum 형태로 deserialize 하지 못하는 경우 발생
	 * 또한, 숫자가 int 혹은 long type 등의 범위를 벗어나는 경우 발생
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected ApiResponse handleHttpMessageNotReadableExceptionException(
			HttpMessageNotReadableException e) {
		log.error("handleHttpMessageNotReadableExceptionException {}", e.getMessage());
		final ErrorResponse response = ErrorResponse.of(e);
		return ApiResponse.onFailure(response, HttpStatus.BAD_REQUEST);
	}

	/**
	 * 지원하지 않은 HTTP method 호출 할 경우 발생
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	protected ApiResponse handleHttpRequestMethodNotSupportedException(
			HttpRequestMethodNotSupportedException e) {
		log.error("handleHttpRequestMethodNotSupportedException {}", e.getMessage());
		final ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
		return ApiResponse.onFailure(response, HttpStatus.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(CommonException.class)
	public ApiResponse handleCommonException(final CommonException e) {
		log.error("CommonException {}", e.getMessage());
		final ErrorResponse response = ErrorResponse.of(e);
		return ApiResponse.onFailure(response, HttpStatus.valueOf(response.getStatus()));
	}

	@ExceptionHandler(Exception.class)
	protected ApiResponse handleUnExpectException(Exception e) {
		log.error("UnExpectException {}", e.getMessage());
		final ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
		return ApiResponse.onFailure(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}