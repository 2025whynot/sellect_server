package com.sellect.server.order.domain;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.order.repository.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Orders {

    private final Long id;
    private final User user;
    private final UserReceivedCoupon userReceivedCoupon;
    private final BigDecimal totalPrice;
    private final String orderNumber;
    private final OrderStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    public static Orders register(User user, BigDecimal totalPrice, OrderStatus status) {
        return Orders.builder()
            .user(user)
            .totalPrice(totalPrice)
            .orderNumber(UUID.randomUUID().toString().replace("-", "").toUpperCase())
            .status(status)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // 주문 상태 변경
    public Orders completeOrder() {
        // 앞서 락으로 막지만 그래도 한번 더!
        if (this.status == OrderStatus.COMPLETED) {
            throw new CommonException(BError.FAIL_FOR_REASON, "complete order",
                "order status is already COMPLETED");
        }
        return Orders.builder()
            .id(this.id)
            .user(this.user)
            .userReceivedCoupon(this.userReceivedCoupon)
            .totalPrice(this.totalPrice)
            .orderNumber(this.orderNumber)
            .status(OrderStatus.COMPLETED)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    public Orders rollbackOrder() {
        return Orders.builder()
            .id(this.id)
            .user(this.user)
            .userReceivedCoupon(this.userReceivedCoupon)
            .totalPrice(this.totalPrice)
            .orderNumber(this.orderNumber)
            .status(OrderStatus.PENDING)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    public Orders failPending() {
        if (this.status != OrderStatus.PENDING) {
            throw new CommonException(BError.NOT_VALID, "PENDING 상태에서만 롤백 가능");
        }

        return Orders.builder()
            .id(this.id)
            .user(this.user)
            .userReceivedCoupon(this.userReceivedCoupon)
            .totalPrice(this.totalPrice)
            .orderNumber(this.orderNumber)
            .status(OrderStatus.FAILED_PENDING)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    public Orders failComplete() {
        if (this.status != OrderStatus.COMPLETED) {
            throw new CommonException(BError.NOT_VALID, "COMPLETED 상태에서만 롤백 가능");
        }

        return Orders.builder()
            .id(this.id)
            .user(this.user)
            .userReceivedCoupon(this.userReceivedCoupon)
            .totalPrice(this.totalPrice)
            .orderNumber(this.orderNumber)
            .status(OrderStatus.FAILED_COMPLETED)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    // 쿠폰 적용의 경우 후순위로 미뤄짐. [03-21 이후로]
    public Orders applyCoupon(UserReceivedCoupon coupon) {
        validateCoupon(coupon);
        return Orders.builder()
            .id(this.id)
            .user(this.user)
            .userReceivedCoupon(coupon)
            // 할인 금액 차감
            .totalPrice(this.totalPrice.subtract(
                BigDecimal.valueOf(coupon.getCoupon().getDiscountCost())))
            .orderNumber(this.orderNumber)
            .status(this.status)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    // 쿠폰 유효성 검사
    private void validateCoupon(UserReceivedCoupon coupon) {
        if (coupon.getIsUsed()) {
            throw new CommonException(BError.COUPON_ALREADY_USED, String.valueOf(coupon.getId()));
        }
        if (!coupon.getUser().getId().equals(this.user.getId())) {
            throw new CommonException(BError.ACCESS_DENIED, "coupon");
        }
    }

    // 주문이 PENDING 상태인지 확인
    public void validatePending() {
        if (this.status != OrderStatus.PENDING) {
            throw new CommonException(BError.FAIL_FOR_REASON, "order validation",
                "order status is not PENDING");
        }
    }

    // 주문이 완료 상태인지 확인
    public void validateCompleted() {
        if (this.status != OrderStatus.COMPLETED) {
            throw new CommonException(BError.FAIL_FOR_REASON, "order validation",
                "order status is not COMPLETED");
        }
    }

    // 주문 소유자 확인
    public void validateOwner(User user) {
        if (!this.user.getId().equals(user.getId())) {
            throw new CommonException(BError.ACCESS_DENIED, "order");
        }
    }

    // OrderServiceV1After 부터 사용
    public void validateNotCompleted() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new CommonException(BError.FAIL_FOR_REASON, "order validation",
                "order status is COMPLETED");
        }
    }
}