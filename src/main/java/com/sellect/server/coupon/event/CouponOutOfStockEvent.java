package com.sellect.server.coupon.event;

import com.sellect.server.coupon.domain.Coupon;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CouponOutOfStockEvent {

    private final Coupon coupon;

    public CouponOutOfStockEvent(Coupon coupon) {
        this.coupon = coupon;
    }
}
