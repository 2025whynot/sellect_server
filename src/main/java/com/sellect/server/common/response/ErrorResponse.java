package com.sellect.server.common.response;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.ErrorCode;
import jakarta.validation.ConstraintViolation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

	private String message;
	private int status;
	private List<FieldError> errors;
	private String code;

	private ErrorResponse(final ErrorCode code, final List<FieldError> errors) {
		this.message = code.getMessage();
		this.status = code.getStatus();
		this.errors = errors;
		this.code = code.getCode();
	}

	private ErrorResponse(final ErrorCode code) {
		this.message = code.getMessage();
		this.status = code.getStatus();
		this.code = code.getCode();
		this.errors = new ArrayList<>();
	}

	public static ErrorResponse of(final ErrorCode code, final BindingResult bindingResult) {
		return new ErrorResponse(code, FieldError.of(bindingResult));
	}

	public static ErrorResponse of(final ErrorCode code, final Set<ConstraintViolation<?>> constraintViolations) {
		return new ErrorResponse(code, FieldError.of(constraintViolations));
	}

	public static ErrorResponse of(final ErrorCode code) {
		return new ErrorResponse(code);
	}

	public static ErrorResponse of(final ErrorCode code, final List<FieldError> errors) {
		return new ErrorResponse(code, errors);
	}

	public static ErrorResponse of(MethodArgumentTypeMismatchException e) {
		final String value = e.getValue() == null ? "" : e.getValue().toString();
		final List<FieldError> errors = FieldError.of(e.getName(), value, e.getErrorCode());
		return new ErrorResponse(ErrorCode.INVALID_TYPE_VALUE, errors);
	}

	public static ErrorResponse of(HttpMessageNotReadableException e) {
		return new ErrorResponse(ErrorCode.INVALID_TYPE_VALUE);
	}

	public static ErrorResponse of(CommonException e) {
		if (e.isBError()) {
			final String reason = e.getMessage() == null ? "" : e.getMessage();
			final List<FieldError> errors = FieldError.of("Business Error", e.getCode(), reason);
			return new ErrorResponse(ErrorCode.BUSINESS_ERROR, errors);
		}
		if (e.isIError()) {
			final String reason = e.getMessage() == null ? "" : e.getMessage();
			final List<FieldError> errors = FieldError.of("Internal Error", e.getCode(), reason);
			return new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, errors);
		}

		return new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class FieldError {
		private String field;
		private String value;
		private String reason;

		private FieldError(final String field, final String value, final String reason) {
			this.field = field;
			this.value = value;
			this.reason = reason;
		}

		public static List<FieldError> of(final String field, final String value, final String reason) {
			List<FieldError> fieldErrors = new ArrayList<>();
			fieldErrors.add(new FieldError(field, value, reason));
			return fieldErrors;
		}

		private static List<FieldError> of(final BindingResult bindingResult) {
			final List<org.springframework.validation.FieldError> fieldErrors = bindingResult.getFieldErrors();
			return fieldErrors.stream()
				.map(error -> new FieldError(
					error.getField(),
					error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
					error.getDefaultMessage()))
				.collect(Collectors.toList());
		}

		private static List<FieldError> of(final Set<ConstraintViolation<?>> constraintViolations) {
			final List<FieldError> fieldErrors = new ArrayList<>();
			constraintViolations.forEach(violation -> {
				fieldErrors.add(new FieldError(
						violation.getPropertyPath().toString(),
						violation.getInvalidValue().toString(),
						violation.getMessage()
				));
			});
			return fieldErrors;
		}
	}
}