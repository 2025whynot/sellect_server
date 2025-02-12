package com.sellect.server.common.exception.enums;

public interface Error {

  String getCode();

  String getMessage(String... values);
}
