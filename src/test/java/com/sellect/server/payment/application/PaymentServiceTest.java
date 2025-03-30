package com.sellect.server.payment.application;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.order.Infrastructure.port.FakePayClient;
import com.sellect.server.order.Infrastructure.port.PayClient;
import com.sellect.server.payment.controller.response.PaymentHistoryResponse;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.FakePaymentRepository;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private PaymentService paymentService;
    private PaymentRepository paymentRepository;

    private PayClient payClient;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private KafkaProducer kafkaProducer;

    private User user;
    private Payment payment;
    private static String USER_UUID = "test-uuid";

    @BeforeEach
    void setUp() {
        paymentRepository = new FakePaymentRepository();
        payClient = new FakePayClient();

        user = User.builder()
            .id(1L)
//            .uuid(USER_UUID)
            .build();
        payment = Payment.ready(1032L, 123L, 1L, 1000, "test-tid");
        paymentService = new PaymentService(
            payClient,
            paymentRepository,
            redisTemplate,
            kafkaProducer
        );
    }

    @Nested
    @DisplayName("getPaymentHistory()")
    class GetPaymentHistory {

        @Test
        @DisplayName("결제 내역 조회 성공")
        void getPaymentHistory_Success() {
            // given
            paymentRepository.save(payment); // 테스트용 결제 데이터 저장

            // when
            Pageable pageable = PageRequest.of(0, 5, Direction.DESC, "createdAt");
            List<PaymentHistoryResponse> paymentHistory = paymentService.getPaymentHistory(user, pageable);

            // then
            assertNotNull(paymentHistory);
            assertEquals(1, paymentHistory.size());
        }

        @Test
        @DisplayName("결제 내역 조회 실패 - 결제 데이터 없음")
        void getPaymentHistory_Failure_NoPaymentData() {
            // when & then
            Pageable pageable = PageRequest.of(0, 5, Direction.DESC, "createdAt");
            List<PaymentHistoryResponse> paymentHistory = paymentService.getPaymentHistory(user, pageable);
            assertEquals(0, paymentHistory.size());
        }
    }
}

