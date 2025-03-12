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
    NOT_SUPPORTED("NOT_SUPPORTED", "%1 not supported"),
    NOT_REGISTERED("NOT_REGISTERED", "%1 not registered"),

    NOT_SELLER("NOT_SELLER", "%1 is not a seller"),
    NOT_USER("NOT_USER", "%1 is not a user"),
    COUPON_QUANTITY_ZERO("COUPON_QUANTITY_ZERO", "The quantity of the coupon%1 is 0"),
    ALREADY_RECEIVED("COUPON_ALREADY_REGISTERED", "The coupon%1 has already been registered"),
    PAYMENT_FAILED("PAYMENT_FAIL", "%1"),
    ACCESS_DENIED("NOT_ACCESSIBLE", "access denied to %1"),
    COUPON_EXPIRED("COUPON_EXPIRED", "The coupon%1 has expired"),
    KAKKO_READY_FAIL("READY_FAIL", "kakao pay ready fail"),
    KAKKO_APPROVE_FAIL("APPROVE_FAIL", "kakao pay approve fail"),
    LOCK_ACQUISITION_FAILED("LOCK_ACQUISITION_FAIL", "lock");


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

