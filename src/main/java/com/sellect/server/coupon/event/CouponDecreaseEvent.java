package com.sellect.server.coupon.event;

import lombok.Getter;

@Getter
public class CouponDecreaseEvent {
    private final Long couponId;

    public CouponDecreaseEvent(Long couponId) {
        this.couponId = couponId;
    }

}
