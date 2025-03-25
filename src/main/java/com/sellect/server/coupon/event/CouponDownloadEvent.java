package com.sellect.server.coupon.event;

import lombok.Getter;

@Getter
public class CouponDownloadEvent {

    private final Long couponId;

    public CouponDownloadEvent(Long couponId) {
        this.couponId = couponId;
    }
}
