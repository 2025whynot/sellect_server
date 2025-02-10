package com.sellect.server.common.exception.handler;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.ErrorCode;
import com.sellect.server.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * @RequestBody, @RequestPart로 요청 바인딩 중 발생하는 유효성 검증 예외 처리
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.error("handleMethodArgumentNotValidException: {}", e.getMessage());
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * @RequestParam, @PathVariable 등의 컨트롤러 파라미터 바인딩 중 발생하는 예외 처리
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	protected ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
		log.error("handleConstraintViolationException: {}", e.getMessage());
		Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, constraintViolations);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * 잘못된 타입이 전달될 때 발생하는 예외 처리 (예: String -> Integer 변환 오류)
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
		log.error("handleMethodArgumentTypeMismatchException: {}", e.getMessage());
		ErrorResponse response = ErrorResponse.of(e);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * JSON 파싱 오류 및 요청 본문 변환 오류 처리
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		log.error("handleHttpMessageNotReadableException: {}", e.getMessage());
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * 지원하지 않는 HTTP 메서드 요청 예외 처리
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
		log.error("handleHttpRequestMethodNotSupportedException: {}", e.getMessage());
		ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
	}

	/**
	 * 인증되지 않은 사용자가 접근할 때 발생하는 예외 처리
	 */
	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
		log.error("handleAccessDeniedException: {}", e.getMessage());
		ErrorResponse response = ErrorResponse.of(ErrorCode.HANDLE_ACCESS_DENIED);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
	}

	/**
	 * 사용자 정의 비즈니스 예외 처리
	 */
	@ExceptionHandler(CommonException.class)
	public ResponseEntity<ErrorResponse> handleCommonException(CommonException e) {
		log.error("handleCommonException: {}", e.getMessage());
		ErrorResponse response = ErrorResponse.of(e);
		return ResponseEntity.status(HttpStatus.valueOf(response.getStatus())).body(response);
	}

	/**
	 * 예측하지 못한 서버 내부 오류 처리
	 */
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
		log.error("handleUnexpectedException: {}", e.getMessage());
		ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}
