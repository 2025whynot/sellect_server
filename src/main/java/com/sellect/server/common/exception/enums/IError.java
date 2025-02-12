package com.sellect.server.common.exception.enums;


import com.sellect.server.common.exception.util.ErrMsgUtil;

public enum IError implements Error {

  RESOURCE_NOT_EXIST("I000", "resource does not exist"),
  RESOURCE_NOT_ALIVE("I001", "resource is not alive"),
  GROUP_NOT_EXIST("I010", "group does not exist"),
  AGENT_NOT_EXIST("I020", "agent does not exist"),
  AGENT_NOT_ALIVE("I021", "agent is not alive"),
  PARAMETER_NOT_EXIST("I030", "parameter does not exist"),
  FIELD_NOT_EXIST("I040", "field does not exist"),
  FIELD_NOT_ALLOWED("I042", "field is not allowed");

  private final String errCode;
  private final String msg;

  @Override
  public String getCode() {
    return this.errCode;
  }

  @Override
  public String getMessage(String... args) {
    return ErrMsgUtil.parseMessage(this.msg, args);
  }

  IError(String errCode, String msg) {
    this.errCode = errCode;
    this.msg = msg;
  }
}

