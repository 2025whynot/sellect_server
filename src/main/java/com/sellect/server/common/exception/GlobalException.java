package com.sellect.server.common.exception;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

	private final ResponseStatus status;

	public GlobalException(ResponseStatus status) {
		this.status = status;
	}
}
