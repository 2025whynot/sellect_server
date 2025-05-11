package com.sellect.server.payment.application;


import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.order.Infrastructure.port.PayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.order.event.message.PaymentApprovalInitMessage;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.controller.response.PaymentHistoryResponse;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.domain.PaymentStatus;
import com.sellect.server.payment.repository.FakePaymentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private PaymentService sut;
    private FakePaymentRepository paymentRepository;

    @Mock
    private PayClient payClient;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private KafkaProducer kafkaProducer;

    @Captor
    private ArgumentCaptor<String> stringCaptor;
    @Captor
    private ArgumentCaptor<ApproveRequest> approveRequestCaptor;
    @Captor
    private ArgumentCaptor<PaymentApprovalInitMessage> paymentApprovalInitMessageCaptor;
    @Captor
    private ArgumentCaptor<KakaoPayReadyRequest> kakaoPayReadyRequestCaptor;

    private User testUser;
    private final Long TEST_ORDER_ID = 1L;
    private final Long TEST_USER_ID = 1L;
    private final int TEST_PRICE = 10000;
    private final Long TEST_PID = 1234567890L;
    private final String TEST_TID = "T12345678901234567890";
    private final String TEST_PG_TOKEN = "test_pg_token";
    private final String KAKAO_REDIRECT_URL = "http://kakao.pay/redirect_url";

    @BeforeEach
    void setUp() {
        paymentRepository = new FakePaymentRepository();
        sut = new PaymentService(payClient, paymentRepository, redisTemplate, kafkaProducer);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        testUser = User.builder()
                .id(TEST_USER_ID)
                .nickname("testUser")
                .build();
    }

    @AfterEach
    void tearDown() {
        reset(payClient, redisTemplate, valueOperations, kafkaProducer);
        paymentRepository.clear();
    }

    @Nested
    @DisplayName("getPaymentHistory 테스트")
    class GetPaymentHistoryTests {
        @Test
        @DisplayName("성공: 사용자 결제 내역 조회")
        void getPaymentHistory_Success() {
            LocalDateTime now = LocalDateTime.now();
            Payment payment1 = Payment.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .price(1000)
                    .createdAt(now.minusDays(1))
                    .status(PaymentStatus.APPROVE)
                    .build();
            Payment payment2 = Payment.builder()
                    .id(2L)
                    .userId(TEST_USER_ID)
                    .price(2000)
                    .createdAt(now)
                    .status(PaymentStatus.APPROVE)
                    .build();
            Payment savedPayment1 = paymentRepository.save(payment1);
            Payment savedPayment2 = paymentRepository.save(payment2);


            Pageable pageable = PageRequest.of(0, 10);

            List<PaymentHistoryResponse> result = sut.getPaymentHistory(testUser, pageable);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).price()).isEqualTo(savedPayment2.getPrice().toString());
            assertThat(result.get(1).price()).isEqualTo(savedPayment1.getPrice().toString());
        }

        @Test
        @DisplayName("성공: 결제 내역 없는 경우 빈 리스트 반환")
        void getPaymentHistory_NoHistory_ReturnsEmptyList() {
            Pageable pageable = PageRequest.of(0, 10);
            List<PaymentHistoryResponse> result = sut.getPaymentHistory(testUser, pageable);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("preparePayment 테스트")
    class PreparePaymentTests {
        private KakaoPayReadyResponse mockKakaoPayReadyResponse;

        @BeforeEach
        void setUpPreparePayment() {
            mockKakaoPayReadyResponse = KakaoPayReadyResponse.builder()
                    .tid(TEST_TID)
                    .next_redirect_pc_url(KAKAO_REDIRECT_URL)
                    .created_at(LocalDateTime.now().toString())
                    .tms_result(true)
                    .build();

            when(payClient.createKakaoPayReadyRequest(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
                    .thenReturn(mock(KakaoPayReadyRequest.class));
            when(payClient.readyPayment(any(KakaoPayReadyRequest.class)))
                    .thenReturn(mockKakaoPayReadyResponse);
        }

        @Test
        @DisplayName("성공: 결제 준비 요청 및 Redis 저장")
        void preparePayment_Success() {
            sut.preparePayment(TEST_USER_ID, TEST_ORDER_ID, TEST_PRICE);

            verify(payClient).createKakaoPayReadyRequest(
                    eq(String.valueOf(TEST_ORDER_ID)),
                    eq(String.valueOf(TEST_USER_ID)),
                    eq("test"),
                    eq(0),
                    eq(TEST_PRICE),
                    stringCaptor.capture()
            );
            String generatedPidStr = stringCaptor.getValue();
            Long generatedPid = Long.valueOf(generatedPidStr);

            verify(payClient).readyPayment(any(KakaoPayReadyRequest.class));

            Optional<Payment> savedPaymentOpt = paymentRepository.findByPid(generatedPid);
            assertThat(savedPaymentOpt).isPresent();
            Payment savedPayment = savedPaymentOpt.get();

            assertThat(savedPayment.getPid()).isEqualTo(generatedPid);
            assertThat(savedPayment.getOrdersId()).isEqualTo(TEST_ORDER_ID);
            assertThat(savedPayment.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(savedPayment.getPrice()).isEqualTo(TEST_PRICE);
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(savedPayment.getTid()).isEqualTo(TEST_TID);
            assertThat(savedPayment.getCreatedAt()).isNotNull();
            assertThat(savedPayment.getUpdatedAt()).isNotNull();

            String expectedRedisKey = "pay-ready:redirect:" + TEST_ORDER_ID;
            verify(valueOperations).set(expectedRedisKey, KAKAO_REDIRECT_URL);
            verify(redisTemplate).expire(expectedRedisKey, 10, TimeUnit.MINUTES);
        }

        @Test
        @DisplayName("실패: KakaoPay readyPayment 요청 실패 시 예외 발생")
        void preparePayment_KakaoPayFails_ThrowsException() {
            when(payClient.readyPayment(any(KakaoPayReadyRequest.class)))
                    .thenThrow(new RuntimeException("KakaoPay API error"));

            assertThatThrownBy(() -> sut.preparePayment(TEST_USER_ID, TEST_ORDER_ID, TEST_PRICE))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("KakaoPay API error");

            assertThat(paymentRepository.findByPid(TEST_PID)).isEmpty();
            verify(valueOperations, never()).set(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("getPaymentUrl 테스트")
    class GetPaymentUrlTests {
        private final String REDIS_KEY_PREFIX = "pay-ready:redirect:";
        private final String RETRY_KEY_PREFIX = "pay-ready:retry-count:";

        @Test
        @DisplayName("성공: Redis에 URL 있고, 첫 시도")
        void getPaymentUrl_UrlInRedis_FirstAttempt_Success() {
            String redirectUrlKey = REDIS_KEY_PREFIX + TEST_ORDER_ID;
            String retryCountKey = RETRY_KEY_PREFIX + TEST_USER_ID + ":" + TEST_ORDER_ID;

            when(valueOperations.increment(retryCountKey, 1L)).thenReturn(1L);
            when(valueOperations.get(redirectUrlKey)).thenReturn(KAKAO_REDIRECT_URL);

            String resultUrl = sut.getPaymentUrl(testUser, TEST_ORDER_ID);

            assertThat(resultUrl).isEqualTo(KAKAO_REDIRECT_URL);
            verify(redisTemplate).expire(retryCountKey, 10, TimeUnit.MINUTES);
        }

        @Test
        @DisplayName("실패: 최대 재시도 횟수 초과")
        void getPaymentUrl_MaxRetryExceeded_ThrowsException() {
            String retryCountKey = RETRY_KEY_PREFIX + TEST_USER_ID + ":" + TEST_ORDER_ID;
            when(valueOperations.increment(retryCountKey, 1L)).thenReturn(6L);

            assertThatThrownBy(() -> sut.getPaymentUrl(testUser, TEST_ORDER_ID))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.FAIL_FOR_REASON.getMessage("get redirect URL from Redis", "Max retry count exceeded"));
        }
    }

    @Nested
    @DisplayName("initPaymentApproval 테스트")
    class InitPaymentApprovalTests {
        @Test
        @DisplayName("성공: 결제 승인 초기화 메시지 발행")
        void initPaymentApproval_Success() {
            Payment readyPayment = Payment.builder()
                    .pid(TEST_PID)
                    .ordersId(TEST_ORDER_ID)
                    .userId(TEST_USER_ID)
                    .status(PaymentStatus.READY)
                    .price(TEST_PRICE)
                    .tid(TEST_TID)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            Payment savedReadyPayment = paymentRepository.save(readyPayment);

            sut.initPaymentApproval(TEST_PID, TEST_PG_TOKEN);

            verify(kafkaProducer).produce(eq("payment-approval-init"), paymentApprovalInitMessageCaptor.capture());
            PaymentApprovalInitMessage sentMessage = paymentApprovalInitMessageCaptor.getValue();

            assertThat(sentMessage.getOrderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(sentMessage.getPid()).isEqualTo(TEST_PID);
            assertThat(sentMessage.getToken()).isEqualTo(TEST_PG_TOKEN);
            assertThat(sentMessage.getPayment().getPid()).isEqualTo(savedReadyPayment.getPid());
            assertThat(sentMessage.getPayment().getId()).isEqualTo(savedReadyPayment.getId());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 결제(pid)에 대한 초기화 시도")
        void initPaymentApproval_PaymentNotFound_ThrowsException() {
            assertThatThrownBy(() -> sut.initPaymentApproval(TEST_PID, TEST_PG_TOKEN))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("payment"));
            verify(kafkaProducer, never()).produce(anyString(), any());
        }
    }


    @Nested
    @DisplayName("approvePayment 테스트")
    class ApprovePaymentTests {
        private Payment readyPaymentInDb;

        @BeforeEach
        void setUpApprovePayment() {
            Payment initialReadyPayment = Payment.ready(
                    TEST_ORDER_ID,
                    TEST_PID,
                    TEST_USER_ID,
                    TEST_PRICE,
                    TEST_TID
            );
            readyPaymentInDb = paymentRepository.save(initialReadyPayment);

            KakaoPayApproveResponse kakaoResponse = KakaoPayApproveResponse.builder()
                    .aid("test_aid")
                    .tid(TEST_TID)
                    .cid("TC0ONETIME")
                    .partnerOrderId(String.valueOf(TEST_ORDER_ID))
                    .partnerUserId(String.valueOf(TEST_USER_ID))
                    .paymentMethodType("MONEY")
                    .itemName("test")
                    .quantity(0)
                    .amount(new KakaoPayApproveResponse.Amount(TEST_PRICE, 0, 0, 0, 0, 0))
                    .createdAt(LocalDateTime.now())
                    .approvedAt(LocalDateTime.now())
                    .build();
            when(payClient.paymentApprove(any(ApproveRequest.class))).thenReturn(kakaoResponse);
        }

        @Test
        @DisplayName("성공: 결제 승인 처리")
        void approvePayment_Success() {
            sut.approvePayment(TEST_PID, TEST_PG_TOKEN);

            Payment approvedPayment = paymentRepository.findByPid(TEST_PID)
                    .orElseThrow(() -> new AssertionError("Payment with PID " + TEST_PID + " not found after approval."));

            assertThat(approvedPayment.getId()).isEqualTo(readyPaymentInDb.getId());
            assertThat(approvedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVE);
            assertThat(approvedPayment.getUpdatedAt()).isNotNull();
            assertThat(approvedPayment.getCreatedAt()).isEqualTo(readyPaymentInDb.getCreatedAt());

            verify(payClient).paymentApprove(approveRequestCaptor.capture());
            ApproveRequest sentApproveRequest = approveRequestCaptor.getValue();
            assertThat(sentApproveRequest.tid()).isEqualTo(TEST_TID);
        }

        @Test
        @DisplayName("실패: 승인된 결제 정보 DB 저장 실패 시 예외 발생")
        void approvePayment_SaveApprovedStatusFails_ThrowsException() {
            // FakePaymentRepository에 예외 발생 조건 설정
            paymentRepository.setNextSaveToFail(
                    readyPaymentInDb.getId(),
                    PaymentStatus.APPROVE,
                    new RuntimeException("DB save failed for approved payment!")
            );

            assertThatThrownBy(() -> sut.approvePayment(TEST_PID, TEST_PG_TOKEN))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB save failed for approved payment!");

            verify(payClient, never()).paymentApprove(any(ApproveRequest.class));

            Payment paymentAfterFailedSave = paymentRepository.findByPid(TEST_PID).orElseThrow();
            assertThat(paymentAfterFailedSave.getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(paymentAfterFailedSave.getId()).isEqualTo(readyPaymentInDb.getId());
        }


        @Test
        @DisplayName("실패: 존재하지 않는 결제(pid)에 대한 승인 시도")
        void approvePayment_PaymentNotFound_ThrowsException() {
            Long nonExistentPid = 999L;
            assertThatThrownBy(() -> sut.approvePayment(nonExistentPid, TEST_PG_TOKEN))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("payment"));
            verify(payClient, never()).paymentApprove(any(ApproveRequest.class));
        }

        @Test
        @DisplayName("실패: KakaoPay approve 요청 실패 시 예외 발생")
        void approvePayment_KakaoPayFails_ThrowsException() {
            when(payClient.paymentApprove(any(ApproveRequest.class)))
                    .thenThrow(new RuntimeException("KakaoPay API approval error"));

            assertThatThrownBy(() -> sut.approvePayment(TEST_PID, TEST_PG_TOKEN))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("KakaoPay API approval error");

            Payment paymentAfterCall = paymentRepository.findByPid(TEST_PID).orElseThrow();
            assertThat(paymentAfterCall.getStatus()).isEqualTo(PaymentStatus.APPROVE);
        }
    }

    @Nested
    @DisplayName("processPaymentApprovalFailure 테스트")
    class ProcessPaymentApprovalFailureTests {
        private Payment approvedPaymentInDb;

        @BeforeEach
        void setUpRollbackPayment() {
            Payment tempReady = Payment.ready(
                    TEST_ORDER_ID,
                    TEST_PID,
                    TEST_USER_ID,
                    TEST_PRICE,
                    TEST_TID
            );
            Payment tempSavedReady = paymentRepository.save(tempReady);
            approvedPaymentInDb = paymentRepository.save(tempSavedReady.approve());
        }

        @Test
        @DisplayName("성공: 결제승인 실패 처리")
        void processPaymentApprovalFailure_Success() {
            sut.processPaymentApprovalFailure(TEST_PID);

            Payment rolledBackPayment = paymentRepository.findByPid(TEST_PID).orElseThrow();
            assertThat(rolledBackPayment.getId()).isEqualTo(approvedPaymentInDb.getId());
            assertThat(rolledBackPayment.getStatus()).isEqualTo(PaymentStatus.FAIL_APPROVE);
            assertThat(rolledBackPayment.getUpdatedAt()).isNotNull();
            assertThat(rolledBackPayment.getCreatedAt()).isEqualTo(approvedPaymentInDb.getCreatedAt());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 결제(pid)에 대한 실패 처리 시도")
        void processPaymentApprovalFailure_PaymentNotFound_ThrowsException() {
            assertThatThrownBy(() -> sut.processPaymentApprovalFailure(999L))
                    .isInstanceOf(CommonException.class)
                    .hasMessage(BError.NOT_EXIST.getMessage("payment"));
        }

        @Test
        @DisplayName("실패: READY 상태 결제를 실패 처리 시도 시 예외 발생")
        void processPaymentApprovalFailure_ReadyPayment_ThrowsExceptionFromDomain() {
            Payment readyPayment = paymentRepository.save(Payment.ready(
                    TEST_ORDER_ID, TEST_PID + 1, TEST_USER_ID, TEST_PRICE, "T_READY_ONLY"
            ));

            assertThatThrownBy(() -> sut.processPaymentApprovalFailure(readyPayment.getPid()))
                    .isInstanceOf(CommonException.class)
                    .hasMessageContaining(BError.FAIL_FOR_REASON.getMessage("Payment.failApprove()", "payment status is not APPROVE"));
        }
    }
}

