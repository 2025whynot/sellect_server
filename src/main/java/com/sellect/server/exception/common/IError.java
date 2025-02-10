package com.sellect.server.exception.common;

// for Internal Error (not for user api)
public enum IError implements Error {

	RESOURCE_NOT_EXIST("I000", "resource is not Exist"),
	RESOURCE_NOT_ALIVE("I001", "resource is not Alive"),
	GROUP_NOT_EXIST("I010", "group is not Exist"),
	AGENT_NOT_EXIST("I020", "agent is not Exist"),
	AGENT_NOT_ALIVE("I021", "agent is not Alive"),
	PARAMETER_NOT_EXIST("I030", "parameter is not Exist"),
	FIELD_NOT_EXIST("I040", "field is not Exist"),
	FIELD_NOT_ALLOWED("I042", "field is not Allowed");

	private String errCode;
	private String msg;

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

