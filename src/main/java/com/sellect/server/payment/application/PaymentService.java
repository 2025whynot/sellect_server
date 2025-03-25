package com.sellect.server.payment.application;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.controller.response.PaymentHistoryResponse;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.message.PayApproveMessage;
import com.sellect.server.payment.event.message.PayReadyMessage;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private static final String REDIS_KEY_PREFIX = "pay-ready:redirect:";

    private final KakaoPayClient kakaoPayClient;
    private final PaymentRepository paymentRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory(User user, Pageable pageable) {
//        Page<Payment> paymentHistoryByUser = paymentRepository.findPaymentHistoryByUser(user.getUuid(), pageable);
        Page<Payment> paymentHistoryByUser = paymentRepository.findPaymentHistoryByUser(
            user.getId(), pageable);

        return paymentHistoryByUser.getContent().stream()
            .map(PaymentHistoryResponse::of)
            .toList();
    }

    @Transactional
    public void preparePayment(final Long orderId, final Long userId, final int totalPrice) {
        Long pid = generatePid();
        KakaoPayReadyResponse kakaoPayReadyResponse = requestKakaoPayReady(pid, orderId, userId, totalPrice);

        savePayReadyStatus(pid, orderId, userId, totalPrice, kakaoPayReadyResponse);

        storePaymentUrlInRedis(orderId, kakaoPayReadyResponse);
    }

    @Transactional
    public void approvePayment(final Long pid, final String token) {

        Payment payment = findPayment(pid);
        Payment approved = savePaymentApproved(payment);

        requestKakaoPayApprove(approved, token);
    }

    // === private method === //

    private Long generatePid() {
        return TsidCreator.getTsid().toLong();
    }

    private KakaoPayReadyResponse requestKakaoPayReady(Long pid, Long orderId, Long userId, int totalPrice) {
        KakaoPayReadyRequest request = kakaoPayClient.createKakaoPayReadyRequest(
            String.valueOf(orderId),
            String.valueOf(userId),
            "test",
            0,
            totalPrice,
            String.valueOf(pid)
        );
        return kakaoPayClient.readyPayment(request);
    }

    private void savePayReadyStatus(Long pid, Long orderId, Long userId, int totalPrice,
        KakaoPayReadyResponse kakaoPayReadyResponse) {
        Payment payment = Payment.ready(
            orderId,
            pid,
            userId,
            totalPrice,
            kakaoPayReadyResponse.tid()
        );
        paymentRepository.save(payment);
    }

    private void storePaymentUrlInRedis(Long orderId, KakaoPayReadyResponse response) {
        String orderIdKey = REDIS_KEY_PREFIX + orderId;
        redisTemplate.opsForValue().set(orderIdKey, response.next_redirect_pc_url());
        redisTemplate.expire(orderIdKey, 10, TimeUnit.MINUTES);
    }


    private Payment findPayment(Long pid) {
        return paymentRepository.findByPid(pid)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "payment"));
    }

    private Payment savePaymentApproved(Payment payment) {
        Payment approved = payment.approve();
        paymentRepository.save(approved);
        return approved;
    }

    private KakaoPayApproveResponse requestKakaoPayApprove(Payment payment, String token) {
        ApproveRequest approveRequest = ApproveRequest.builder()
            .cid("TC0ONETIME")
            .tid(payment.getTid())
            .partnerOrderId(String.valueOf(payment.getOrdersId()))
//            .partnerUserId(approvePayment.getUid())
            .partnerUserId(String.valueOf(payment.getUserId()))
            .pgToken(token)
            .build();

        return kakaoPayClient.paymentApprove(approveRequest);
    }

}

