package com.sellect.server.exception.common;

public interface Error {
	String getCode();

	String getMessage(String... values);
}
