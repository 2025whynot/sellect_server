package com.sellect.server.coupon.controller.response;


public record ActiveCouponResponse(
    Boolean isRegistered,
    CouponInfo couponInfo
) {


}
