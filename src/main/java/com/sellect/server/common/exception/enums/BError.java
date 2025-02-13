package com.sellect.server.common.exception.enums;

import com.sellect.server.common.exception.util.ErrMsgUtil;

// For Business Logic
public enum BError implements Error {
    REQUIRED("REQUIRED", "%1 is required"),
    NOT_EXIST("NOT_EXIST", "%1 does not exist"),
    EXIST("EXIST", "%1 already exists"),
    NOT_MATCH("NOT_MATCH", "%1 does not match"),
    NOT_VALID("NOT_VALID", "%1 is not valid"),
    NOT_MATCHES("NOT_MATCHES", "%1 and %2 do not match"),
    MATCH("MATCH", "%1 match"),
    MATCHES("MATCHES", "%1 and %2 match"),
    FAIL("FAIL", "%1 failed"),
    SUCCESS("SUCCESS", "%1 succeeded"),
    FAIL_FOR_REASON("FAIL_FOR_REASON", "%1 failed for reason (%2)"),
    NOT_SUPPORT("NOT_SUPPORT", "%1 not supported"),
    NOT_REGISTERED("NOT_REGISTERED", "%1 not registered");

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

    BError(String errCode, String msg) {
        this.errCode = errCode;
        this.msg = msg;
    }
}

