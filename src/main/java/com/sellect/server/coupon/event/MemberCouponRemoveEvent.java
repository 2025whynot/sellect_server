package com.sellect.server.coupon.event;

import lombok.Getter;

@Getter
public class MemberCouponRemoveEvent {
    private final Long couponId;
    private final Long userId;

    public MemberCouponRemoveEvent(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
    }
}
