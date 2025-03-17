package com.sellect.server.payment.domain;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.*;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Nested
    @DisplayName("readyPayment()")
    class PaymentCreateTest{
        @Test
        @DisplayName("[성공] 유효한 입력으로 결제 준비 상태 생성 성공")
        void testReadySuccess() {
            // Given
            String orderId = "order123";
            String pid = "product456";
            String uid = "user789";
            Integer price = 1000;
            String tid = "transaction101";

            // When
            Payment payment = Payment.ready(orderId, pid, 1L, price, tid);

            // Then
            assertNotNull(payment);
            assertEquals(orderId, payment.getOrderId());
            assertEquals(pid, payment.getPid());
//            assertEquals(uid, payment.getUid());
            assertEquals(price, payment.getPrice());
            assertEquals(tid, payment.getTid());
            assertEquals(PaymentStatus.READY, payment.getStatus());
            assertNotNull(payment.getCreatedAt());
            assertNotNull(payment.getUpdatedAt());
        }

        @Test
        @DisplayName("결제 금액이 음수일 때 예외 발생")
        void testReadyWithNegativePrice() {
            // Given
            String orderId = "order123";
            String pid = "product456";
            Long userId = 1L;
            Integer price = -100; // 음수 가격
            String tid = "transaction101";

            // When & Then
            CommonException exception = assertThrows(CommonException.class, () -> {
                Payment.ready(orderId, pid, userId, price, tid);
            });
            assertEquals("결제 금액은 0원 보다 높어야 합니다.", exception.getMessage());
        }

        @Test
        @DisplayName("orderId가 null일 때 예외 발생")
        void testReadyWithNullOrderId() {
            // Given
            String orderId = null; // null 입력
            String pid = "product456";
//            String userId = "user789";
            Long userId = 1L;
            Integer price = 1000;
            String tid = "transaction101";

            // When & Then
            CommonException exception = assertThrows(CommonException.class, () -> {
                Payment.ready(orderId, pid, userId, price, tid);
            });
            assertEquals("결제 정보가 올바르지 않습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("pid가 null일 때 예외 발생")
        void testReadyWithNullPid() {
            // Given
            String orderId = "order123";
            String pid = null; // null 입력
            Long userId = 1L;
            Integer price = 1000;
            String tid = "transaction101";

            // When & Then
            CommonException exception = assertThrows(CommonException.class, () -> {
                Payment.ready(orderId, pid, userId, price, tid);
            });
            assertEquals("결제 정보가 올바르지 않습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("uid가 null일 때 예외 발생")
        void testReadyWithNullUid() {
            // Given
            String orderId = "order123";
            String pid = "product456";
            Long uid = null; // null 입력
            Integer price = 1000;
            String tid = "transaction101";

            // When & Then
            CommonException exception = assertThrows(CommonException.class, () -> {
                Payment.ready(orderId, pid, uid, price, tid);
            });
            assertEquals("결제 정보가 올바르지 않습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("tid가 null일 때 예외 발생")
        void testReadyWithNullTid() {
            // Given
            String orderId = "order123";
            String pid = "product456";
            Long userId  = 1L;
            Integer price = 1000;
            String tid = null; // null 입력

            // When & Then
            CommonException exception = assertThrows(CommonException.class, () -> {
                Payment.ready(orderId, pid, userId, price, tid);
            });
            assertEquals("결제 정보가 올바르지 않습니다.", exception.getMessage());
        }
    }

    @Test
    @DisplayName("결제 승인")
    void approvePayment() {
        Payment payment = Payment.ready("orderId", "pid", 1L, 1000, "tid");
        Payment approvePayment = payment.approvePayment();
        assertEquals(PaymentStatus.APPROVE, approvePayment.getStatus());
    }

    @Nested
    @DisplayName("결제 상태 변경 테스트")
    class PaymentStatusChangeTest{
        @Test
        @DisplayName("[성공] Status.READY -> Status.APPROVE")
        void paymentApproveSuccess() {
            //given
            Payment payment = Payment.ready("orderId", "pid", 1L, 1000, "tid");
            //when
            Payment approvePayment = payment.approvePayment();
            //then
            then(approvePayment.getStatus()).isEqualTo(PaymentStatus.APPROVE);

        }

        @Test
        @DisplayName("[성공] Status.READY -> Status.FAIL")
        void paymentFailSuccess() {
            //given
            Payment payment = Payment.ready("orderId", "pid", 1L, 1000, "tid");
            //when
            Payment failedPayment = payment.failPayment();
            //then
            then(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAIL);

        }

        @Test
        @DisplayName("[성공] Status.READY -> Status.CANCEL")
        void paymentCancelSuccess() {
            //given
            Payment payment = Payment.ready("orderId", "pid", 1L, 1000, "tid");
            //when
            Payment cancelledPayment = payment.cancelPayment();
            //then
            then(cancelledPayment.getStatus()).isEqualTo(PaymentStatus.CANCEL);
        }

        @Test
        @DisplayName("대기 상태가 아닌 경우 approve 호출 시 상태가 변경되지 않음")
        void approvePaymentDoesNotChangeNonPendingStatus() {
            // given
            Payment payment = Payment.ready("orderId", "pid", 1L, 1000, "tid");
            Payment approvePayment = payment.approvePayment();// 먼저 APPROVE로 상태 변경
            PaymentStatus initialStatus = approvePayment.getStatus();

            // when
            Payment result = approvePayment.approvePayment();

            // then
            then(result.getStatus()).isEqualTo(initialStatus);
            then(result.getStatus()).isNotEqualTo(PaymentStatus.READY);
        }

        @Test
        @DisplayName("대기 상태가 아닌 경우 fail 호출 시 상태가 변경되지 않음")
        void failPaymentDoesNotChangeNonPendingStatus() {
            // given
            Payment payment = Payment.ready("orderId", "pid", 1L, 1000, "tid");
            Payment failedPayment = payment.failPayment();// 먼저 CANCEL로 상태 변경
            PaymentStatus initialStatus = failedPayment.getStatus();

            // when

            // then
            then(failedPayment.getStatus()).isEqualTo(initialStatus);
            then(failedPayment.getStatus()).isNotEqualTo(PaymentStatus.READY);
        }

        @Test
        @DisplayName("[실패] Staus.Ready가 아닐떄, failPayment() 호출")
        void throwExceptionWhenStatusReadyCallFailPayment() {
            //given
            Payment payment = Payment.ready("orderId", "pid", 1L, 1000, "tid");
            Payment failedPayment = payment.failPayment();// 먼저 CANCEL로 상태 변경

            //when
            CommonException exception = assertThrows(CommonException.class, () -> {
                Payment twoFailPayment = failedPayment.failPayment();
            });

            //then
            then(exception.getErrorType()).isEqualTo(BError.class);
            then(exception.getMessage()).isEqualTo(
                "failPayment() failed for reason (PaymentStatus is not Ready)");
        }

        @Test
        @DisplayName("[실패] Staus.Ready가 아닐떄, cancelPayment() 호출")
        void throwExceptionWhenStatusReadyCallCancelPayment() {
            //given
            Payment payment = Payment.ready("orderId", "pid", 1L, 1000, "tid");
            Payment failedPayment = payment.failPayment();// 먼저 CANCEL로 상태 변경

            //when
            CommonException exception = assertThrows(CommonException.class, () -> {
                Payment twoFailPayment = failedPayment.cancelPayment();
            });

            //then
            then(exception.getErrorType()).isEqualTo(BError.class);
            then(exception.getMessage()).isEqualTo(
                "cancelPayment() failed for reason (PaymentStatus is not Ready)");
        }
    }
}