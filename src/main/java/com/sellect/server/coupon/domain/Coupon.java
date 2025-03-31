package com.sellect.server.coupon.domain;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.repository.entity.CouponStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {
    private final Long id;
    private final User seller;
    private final Integer discountCost;
    private final Integer quantity;
    private final LocalDate expirationDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;
    private final CouponStatus couponStatus;


    public void validateDownload() {
        if (isOutOfStock()) {
            throw new CommonException(BError.COUPON_QUANTITY_ZERO, String.valueOf(this.id));
        }
        if (isExpired()) {
            throw new CommonException(BError.COUPON_EXPIRED, String.valueOf(this.id));
        }
    }

    public Coupon decreaseQuantity() {
        if (isOutOfStock()) {
            throw new CommonException(BError.COUPON_QUANTITY_ZERO);
        }
        return Coupon.builder()
            .id(this.id)
            .seller(this.seller)
            .discountCost(this.discountCost)
            .quantity(this.quantity - 1)
            .expirationDate(this.expirationDate)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .couponStatus(this.couponStatus)
            .build();
    }

    public Coupon decreaseQuantity(int decreaseQuantity) {
        if (isOutOfStock()) {
            throw new CommonException(BError.COUPON_QUANTITY_ZERO);
        }
        return Coupon.builder()
            .id(this.id)
            .seller(this.seller)
            .discountCost(this.discountCost)
            .quantity(this.quantity - decreaseQuantity)
            .expirationDate(this.expirationDate)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .couponStatus(this.couponStatus)
            .build();
    }

    private boolean isOutOfStock() {
        return (this.quantity <= 0 || this.couponStatus.equals(CouponStatus.OUT_OF_STOCK));
    }

    private boolean isExpired(){
        return this.expirationDate.isBefore(LocalDate.now());
    }

    public Coupon outOfStock() {
        return Coupon.builder()
            .id(this.id)
            .seller(this.seller)
            .discountCost(this.discountCost)
            .quantity(this.quantity)
            .expirationDate(this.expirationDate)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .couponStatus(CouponStatus.OUT_OF_STOCK)
            .build();
    }
}
