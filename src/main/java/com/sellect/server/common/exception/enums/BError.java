package com.sellect.server.common.exception.enums;

import com.sellect.server.common.exception.util.ErrMsgUtil;

// For Business Logic
public enum BError implements Error {

    // common
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
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "[INTERNAL_SERVER_ERROR] - %1"),
    TIMEOUT("TIMEOUT", "[TIMEOUT] - %1"),
    DB_ERROR("DB_ERROR", "[DB_ERROR] - %1"),
    ACCESS_DENIED("ACCESS_DENIED", "access denied to %1"),

    // user
    NOT_SELLER("NOT_SELLER", "%1 is not a seller"),
    NOT_USER("NOT_USER", "%1 is not a user"),

    // coupon
    COUPON_QUANTITY_ZERO("COUPON_QUANTITY_ZERO", "The quantity of the coupon%1 is 0"),
    COUPON_ALREADY_RECEIVED("COUPON_ALREADY_REGISTERED", "The coupon%1 has already been registered"),
    COUPON_ALREADY_USED("COUPON_ALREADY_USED", "The coupon(id=%1) has already been used"),
    COUPON_EXPIRED("COUPON_EXPIRED", "The coupon%1 has expired"),

    // payment
    PAYMENT_FAILED("PAYMENT_FAIL", "%1"),
    KAKAO_READY_FAIL("READY_FAIL", "kakao pay ready fail"),
    KAKAO_APPROVE_FAIL("APPROVE_FAIL", "kakao pay approve fail"),
    COMPENSATION_FAILED("COMPENSATION_FAILED", "%1"),

    // lock
    LOCK_ACQUISITION_FAILED("LOCK_ACQUISITION_FAIL", "lock acquisition failed for %1"),

    // stock
    OUT_OF_STOCK("OUT_OF_STOCK", "%1");


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

