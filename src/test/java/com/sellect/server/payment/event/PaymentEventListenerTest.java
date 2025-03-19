package com.sellect.server.payment.event;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.listener.proxy.PaymentEventListenerProxy;
import com.sellect.server.payment.repository.FakePaymentRepository;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PaymentEventListenerTest {

    private FakePaymentRepository paymentRepository = new FakePaymentRepository();
    private KakaoPayClient kakaoPayClient = mock(KakaoPayClient.class);
    private PaymentEventListenerProxy paymentEventListener;

    @BeforeEach
    void setUp() {
        // 프록시 객체를 수동으로 생성
        paymentEventListener = new PaymentEventListenerProxy(kakaoPayClient, paymentRepository);
    }


    @Nested
    @DisplayName("KakaoPayReadyEvent()는")
    class KakaoPayReadyEventTest {
        @Test
        @DisplayName("카카오 결제 준비 이벤트 생성")
        void willSuccess() throws ExecutionException, InterruptedException {
            //given
            User user = User.builder()
                .id(1L)
//                .uuid("test-uuid")
                .build();

            KakaoPayReadyResponse kakaoPayReadyResponse = KakaoPayReadyResponse.builder()
                .next_redirect_pc_url("redirect_pc_url_success")
                .tid("test-tid")
                .build();

            Orders order = mock(Orders.class);
            CompletableFuture<String> future = new CompletableFuture<>();
            KakaoPayReadyEvent kakaoPayReadyEvent = new KakaoPayReadyEvent(this, user, order, future);

            when(order.getTotalPrice()).thenReturn(BigDecimal.valueOf(1000L));
            when(kakaoPayClient.readyPayment(any())).thenReturn(kakaoPayReadyResponse);

            //when
            paymentEventListener.kakaoPayReadyEvent(kakaoPayReadyEvent);

            //then
            String redirectUrl = future.get();
            then(redirectUrl).isEqualTo("redirect_pc_url_success");
        }

        @Test
        @DisplayName("카카오 결제 준비 이벤트 생성 - 실패 (Exception 발생)")
        void willFailWithException() {
            // given
            User user = User.builder()
                .id(1L)
//                .uuid("test-uuid")
                .build();

            Orders order = mock(Orders.class);
            CompletableFuture<String> future = new CompletableFuture<>();
            KakaoPayReadyEvent kakaoPayReadyEvent = new KakaoPayReadyEvent(this, user, order, future);

            when(order.getTotalPrice()).thenReturn(BigDecimal.valueOf(1000L));
            when(kakaoPayClient.readyPayment(any())).thenThrow(new CommonException(BError.KAKKO_READY_FAIL));

            // when
            paymentEventListener.kakaoPayReadyEvent(kakaoPayReadyEvent);

            // then
            verify(kakaoPayClient, times(1)).readyPayment(any());

            // future가 예외로 완료되었는지 확인
            ExecutionException exception = assertThrows(ExecutionException.class, future::get);
            assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(exception.getCause().getMessage()).isEqualTo("카카오페이 결제 준비 실패: kakao pay ready fail");
        }

    }

    @Nested
    @DisplayName("KakaoPayApproveEvent()는")
    class KakaoPayApproveEventTest{
        @Test
        @DisplayName("[성공] 카카오 결제 승인 이벤트")
        void willSuccess() {
            //given
            Payment payment = Payment.ready(1234L, 123L, 1L, 1000, "tid1");
            KakaoPayApproveEvent kakaoPayApproveEvent = KakaoPayApproveEvent.publish(payment,
                "pgToken123", 123L);
            // 이벤트에 future setter가 있다면 아래와 같이 설정
            // kakaoPayApproveEvent.setFuture(future);

            when(kakaoPayClient.paymentApprove(any())).thenReturn(any());

            // when
            paymentEventListener.kakaoPayApproveEvent(kakaoPayApproveEvent);

            // future.get(); // 성공적으로 완료됨을 확인
            verify(kakaoPayClient, times(1)).paymentApprove(any());
        }


//        @Test
//        @DisplayName("[실패] 카카오 결제 승인 이벤트 - API 호출 실패")
//        void willFail() {
//            // given
//            Payment payment = Payment.ready(1234L, 123L, 1L, 1000, "tid1");
//            KakaoPayApproveEvent kakaoPayApproveEvent = KakaoPayApproveEvent.publish(payment,
//                "pgToken123", 123L);
//
//            when(kakaoPayClient.paymentApprove(any())).thenThrow(new CommonException(BError.KAKKO_APPROVE_FAIL));
//
//            // when
//            CommonException exception = assertThrows(CommonException.class, () -> {
//                paymentEventListener.kakaoPayApproveEvent(kakaoPayApproveEvent);
//            });
//
//            // then
//            assertEquals(BError.KAKKO_APPROVE_FAIL.getMessage(), exception.getMessage());
//            verify(kakaoPayClient, times(1)).paymentApprove(any()); // API 호출 시도
//        }
    }
}
