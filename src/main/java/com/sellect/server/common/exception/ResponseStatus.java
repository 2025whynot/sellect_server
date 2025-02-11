package com.sellect.server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseStatus {

	INTERNAL_SERVER_ERROR(false, "SERVER900", "Internal server error"),
	/*
	    100 : USERS Service Error
	*/

	// Token, Code 예시
	TOKEN_NOT_VALID(false, "AUTH101", "토큰이 유효하지 않습니다.");

	private final boolean isSuccess;
	private final String code;
	private final String message;

	}
