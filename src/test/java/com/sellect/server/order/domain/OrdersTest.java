package com.sellect.server.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OrdersTest {

    private User user;
    private UserReceivedCoupon userReceivedCoupon;
    private BigDecimal totalPrice;
    private String orderNumber;
    private LocalDateTime time;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .id(1L)
            .build();

        Coupon coupon = Coupon.builder()
            .id(1L)
            .discountCost(1000)
            .build();

        userReceivedCoupon = UserReceivedCoupon.builder()
            .id(1L)
            .user(user)
            .coupon(coupon)
            .isUsed(false) // 기본적으로 사용되지 않은 상태로 설정
            .build();

        totalPrice = new BigDecimal("100000");
        time = LocalDateTime.parse("2024-08-01T00:00:00");
        orderNumber = "test-order-number";
    }

    @Test
    @DisplayName("주문 등록 성공")
    void testRegister() {
        // when
        Orders order = Orders.register(user, totalPrice, OrderStatus.PENDING);

        // then
        assertNotNull(order);
        assertEquals(user, order.getUser());
        assertEquals(totalPrice, order.getTotalPrice());
        assertNotNull(order.getOrderNumber());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertNotNull(order.getCreatedAt());
        assertNull(order.getUpdatedAt());
        assertNull(order.getDeleteAt());
    }

    @Nested
    @DisplayName("주문 상태 변경 테스트")
    class ChangeStatusTest {

        @Test
        @DisplayName("주문 상태 변경 성공")
        void testChangeStatus() {
            // given
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .userReceivedCoupon(userReceivedCoupon)
                .totalPrice(totalPrice)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .createdAt(time)
                .updatedAt(time)
                .build();

            // when
            Orders updatedOrder = order.completeOrder();

            // then
            assertNotNull(updatedOrder);
            assertEquals(order.getId(), updatedOrder.getId());
            assertEquals(order.getUser(), updatedOrder.getUser());
            assertEquals(order.getUserReceivedCoupon(), updatedOrder.getUserReceivedCoupon());
            assertEquals(order.getTotalPrice(), updatedOrder.getTotalPrice());
            assertEquals(order.getOrderNumber(), updatedOrder.getOrderNumber());
            assertEquals(order.getCreatedAt(), updatedOrder.getCreatedAt());
            assertNotEquals(order.getUpdatedAt(), updatedOrder.getUpdatedAt());
            assertEquals(order.getDeleteAt(), updatedOrder.getDeleteAt());
        }

        @Test
        @DisplayName("이미 완료된 주문 상태 변경 시 예외 발생")
        void testChangeStatusAlreadyCompleted() {
            // given
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.COMPLETED)
                .createdAt(time)
                .build();

            // when & then
            CommonException exception = assertThrows(CommonException.class,
                () -> order.completeOrder());
            assertEquals(BError.FAIL_FOR_REASON.getMessage("complete order", "order status is already COMPLETED"),
                exception.getMessage());
        }
    }

    @Nested
    @DisplayName("쿠폰 적용 테스트")
    class ApplyCouponTest {

        @Test
        @DisplayName("쿠폰 적용 성공")
        void testApplyCoupon() {
            // given
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .userReceivedCoupon(null)
                .totalPrice(totalPrice)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .createdAt(time)
                .updatedAt(time)
                .build();

            // when
            Orders updatedOrder = order.applyCoupon(userReceivedCoupon);
            int discountCost = userReceivedCoupon.getCoupon().getDiscountCost();

            // then
            assertNotNull(updatedOrder);
            assertEquals(order.getId(), updatedOrder.getId());
            assertEquals(order.getUser(), updatedOrder.getUser());
            assertEquals(userReceivedCoupon, updatedOrder.getUserReceivedCoupon());
            BigDecimal minusCouponPrice = order.getTotalPrice()
                .subtract(BigDecimal.valueOf(discountCost));
            assertEquals(minusCouponPrice, updatedOrder.getTotalPrice());
            assertEquals(order.getOrderNumber(), updatedOrder.getOrderNumber());
            assertEquals(order.getStatus(), updatedOrder.getStatus());
            assertEquals(order.getCreatedAt(), updatedOrder.getCreatedAt());
            assertNotEquals(order.getUpdatedAt(), updatedOrder.getUpdatedAt());
            assertEquals(order.getDeleteAt(), updatedOrder.getDeleteAt());
        }

        @Test
        @DisplayName("이미 사용된 쿠폰 적용 시 예외 발생")
        void testApplyUsedCoupon() {
            // given
            UserReceivedCoupon usedCoupon = UserReceivedCoupon.builder()
                .id(2L)
                .user(user)
                .coupon(Coupon.builder().id(2L).discountCost(500).build())
                .isUsed(true)
                .build();

            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .createdAt(time)
                .build();

            // when & then
            CommonException exception = assertThrows(CommonException.class,
                () -> order.applyCoupon(usedCoupon));
            assertEquals(BError.COUPON_ALREADY_USED.getMessage(String.valueOf(usedCoupon.getId())), exception.getMessage());
        }

        @Test
        @DisplayName("쿠폰 소유자가 아닌 경우 예외 발생")
        void testApplyCouponInvalidOwner() {
            // given
            User anotherUser = User.builder().id(2L).build();
            UserReceivedCoupon anotherUserCoupon = UserReceivedCoupon.builder()
                .id(2L)
                .user(anotherUser)
                .coupon(Coupon.builder().id(2L).discountCost(500).build())
                .isUsed(false)
                .build();

            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .createdAt(time)
                .build();

            // when & then
            CommonException exception = assertThrows(CommonException.class,
                () -> order.applyCoupon(anotherUserCoupon));
            assertEquals(BError.ACCESS_DENIED.getMessage("coupon"), exception.getMessage());
        }
    }

    @Nested
    @DisplayName("유효성 검사 테스트")
    class ValidationTest {

        @Test
        @DisplayName("PENDING 상태 검증 성공")
        void testValidatePendingSuccess() {
            // given
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .build();

            // when & then (예외가 발생하지 않으면 성공)
            order.validatePending();
        }

        @Test
        @DisplayName("PENDING 상태가 아닌 경우 예외 발생")
        void testValidatePendingFailure() {
            // given
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.COMPLETED)
                .build();

            // when & then
            CommonException exception = assertThrows(CommonException.class,
                () -> order.validatePending());
            assertEquals(BError.FAIL_FOR_REASON.getMessage("order validation", "order status is not PENDING"),
                exception.getMessage());
        }

        @Test
        @DisplayName("COMPLETED 상태 검증 성공")
        void testValidateCompletedSuccess() {
            // given
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.COMPLETED)
                .build();

            // when & then (예외가 발생하지 않으면 성공)
            order.validateCompleted();
        }

        @Test
        @DisplayName("COMPLETED 상태가 아닌 경우 예외 발생")
        void testValidateCompletedFailure() {
            // given
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .build();

            // when & then
            CommonException exception = assertThrows(CommonException.class,
                () -> order.validateCompleted());
            assertEquals(BError.FAIL_FOR_REASON.getMessage("order validation", "order status is not COMPLETED"),
                exception.getMessage());
        }

        @Test
        @DisplayName("주문 소유자 검증 성공")
        void testValidateOwnerSuccess() {
            // given
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .build();

            // when & then (예외가 발생하지 않으면 성공)
            order.validateOwner(user);
        }

        @Test
        @DisplayName("주문 소유자가 아닌 경우 예외 발생")
        void testValidateOwnerFailure() {
            // given
            User anotherUser = User.builder().id(2L).build();
            Orders order = Orders.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .build();

            // when & then
            CommonException exception = assertThrows(CommonException.class,
                () -> order.validateOwner(anotherUser));
            assertEquals(BError.ACCESS_DENIED.getMessage("order"), exception.getMessage());
        }
    }
}