package com.sellect.server.common.exception.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorCode {

	// Common
	INVALID_INPUT_VALUE(400, "C001", "Invalid Input Value"),
	INVALID_VALUE_TYPE(400, "C002", "Invalid Value Type"),
	ENTITY_NOT_FOUND(400, "C003", "Entity Not Found"),
	BAD_REQUEST(400, "C004", "Bad Request"),
	UNAUTHORIZED(401, "C005", "Unauthorized"),
	INVALID_TOKEN(401, "C006", "Invalid Token"),
	ACCESS_DENIED(403, "C007", "Access Denied"),
	METHOD_NOT_ALLOWED(405, "C008", "Method Not Allowed"),
	INTERNAL_SERVER_ERROR(500, "C009", "Server Error"),

	// Business
	BUSINESS_ERROR(400, "B000", "Business Error");

	private final int status;
	private final String code;
	private final String message;

	ErrorCode(final int status, final String code, final String message) {
		this.status = status;
		this.message = message;
		this.code = code;
	}

}