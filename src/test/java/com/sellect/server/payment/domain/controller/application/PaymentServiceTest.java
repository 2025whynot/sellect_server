package com.sellect.server.payment.domain.controller.application;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sellect.server.auth.domain.User;
import com.sellect.server.payment.application.PaymentService;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.domain.controller.repository.FakePaymentRepository;
import com.sellect.server.payment.controller.request.PaymentRequest;
import com.sellect.server.payment.controller.response.KakaoPayReadyResponse;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private PaymentService paymentService;
    private PaymentRepository paymentRepository;
    @Mock
    private RestTemplate restTemplate;

    private User user;
    private PaymentRequest paymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRepository = new FakePaymentRepository();
        user = User.builder()
            .id(1L)
            .uuid("test-uuid")
            .build();

        paymentRequest = new PaymentRequest("order-123", "test item", 1, 1000);
        payment = Payment.readyPayment("order-123", "test-pid", "test-uuid", 1000, "test-tid");
        paymentService = new PaymentService(paymentRepository, restTemplate);
    }

    @Nested
    @DisplayName("initialPayment")
    class InitialPayment {

        @Test
        @DisplayName("결제 준비 성공")
        void initialPayment_Success() {
            // Arrange
            KakaoPayReadyResponse mockResponse = KakaoPayReadyResponse.builder()
                .tid("test-tid")
                .next_redirect_pc_url("http://test-redirect-url.com")
                .build();

            when(restTemplate.exchange(any(String.class),
                eq(HttpMethod.POST),
                any(), eq(KakaoPayReadyResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

            // Act
            String redirectUrl = paymentService.initialPayment(user, paymentRequest);

            // Assert
            assertEquals("http://test-redirect-url.com", redirectUrl);
        }

        @Test
        @DisplayName("결제 준비 실패 - 카카오페이 API 오류")
        void initialPayment_Failure() {
            // Arrange
            when(restTemplate.exchange(any(String.class),
                eq(HttpMethod.POST),
                any(), eq(KakaoPayReadyResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

            // Act & Assert
            Exception exception = assertThrows(RuntimeException.class, () -> {
                paymentService.initialPayment(user, paymentRequest);
            });

            assertEquals("Failed to prepare KakaoPay payment", exception.getMessage());
        }

        @Test
        @DisplayName("결제 승인 성공")
        void approvePayment_Success() {
            // Arrange
            paymentRepository.save(payment);
            when(restTemplate.exchange(any(String.class),
                eq(HttpMethod.POST),
                any(), eq(java.util.Map.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

            // Act
            paymentService.approvePayment("test-pid", "test-token");

            // Assert
            Payment approvedPayment = paymentRepository.findByPid("test-pid");
            assertNotNull(approvedPayment);
            assertEquals("test-pid", approvedPayment.getPid());
        }

        @Test
        @DisplayName("결제 승인 실패 - 카카오페이 API 오류")
        void approvePayment_Failure() {
            // Arrange
            paymentRepository.save(payment);
            when(restTemplate.exchange(any(String.class),
                eq(org.springframework.http.HttpMethod.POST),
                any(), eq(java.util.Map.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

            // Act & Assert
            Exception exception = assertThrows(RuntimeException.class, () -> {
                paymentService.approvePayment("test-pid", "test-token");
            });

            assertEquals("Failed to approve KakaoPay payment", exception.getMessage());
        }
    }


    @Nested
    @DisplayName("approvePayment")
    class ApprovePayment {

        @Test
        @DisplayName("결제 승인 성공")
        void approvePayment_Success() {
            // given
            paymentRepository.save(payment); // 테스트용 결제 데이터 저장
            when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(),
                eq(Map.class)
            )).thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));

            // when
            paymentService.approvePayment("test-pid", "test-token");

            // then
            Payment approvedPayment = paymentRepository.findByPid("test-pid");
            assertNotNull(approvedPayment);
            assertEquals("test-pid", approvedPayment.getPid());
        }

        @Test
        @DisplayName("결제 승인 실패 - 카카오페이 API 오류")
        void approvePayment_Failure_KakaoPayApiError() {
            // given
            paymentRepository.save(payment); // 테스트용 결제 데이터 저장
            when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(),
                eq(Map.class)
            )).thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.INTERNAL_SERVER_ERROR));

            // when & then
            Exception exception = assertThrows(RuntimeException.class, () -> {
                paymentService.approvePayment("test-pid", "test-token");
            });
            assertEquals("Failed to approve KakaoPay payment", exception.getMessage());
        }

    }
}
