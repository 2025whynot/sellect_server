package com.sellect.server.payment.event;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.message.PayApproveMessage;
import com.sellect.server.payment.event.message.PayReadyMessage;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentListener {

    private static final String REDIS_KEY_PREFIX = "pay-ready:redirect:";

    private final KakaoPayClient kakaoPayClient;
    private final PaymentRepository paymentRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(topics = "pay-ready", groupId = "pay-ready-group")
    public void payReadyListener(PayReadyMessage message) {
        consumePayReadyMessage(message);
    }

    @KafkaListener(topics = "pay-approve", groupId = "pay-approve-group")
    public void payApproveListener(PayApproveMessage message) {
        consumePayApproveMessage(message);
    }

    // === Dead Letter Queue 처리 === //


    // === private method === //

    private void consumePayReadyMessage(PayReadyMessage message) {
        Long pid = generatePid();

        KakaoPayReadyResponse kakaoPayReadyResponse = requestKakaoPayReady(message, pid);

        savePayReadyStatus(message, pid, kakaoPayReadyResponse);

        storePaymentUrlInRedis(message, kakaoPayReadyResponse);
    }

    private void consumePayApproveMessage(PayApproveMessage message) {
        paymentRepository.findByPid(message.getPid())
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "payment"));

        Payment approved = message.getPayment().approve();
        paymentRepository.save(approved);

        // 결제 승인 요청
        requestKakaoPayApprove(message, approved);
    }

    private Long generatePid() {
        return TsidCreator.getTsid().toLong(); // TODO: tsid로 변경
    }

    private KakaoPayReadyResponse requestKakaoPayReady(PayReadyMessage message, Long pid) {
        KakaoPayReadyRequest request = kakaoPayClient.createKakaoPayReadyRequest(
            String.valueOf(message.getOrderId()),
            String.valueOf(message.getUserId()),
            "test",
            0,
            message.getTotalPrice(),
            String.valueOf(pid)
        );
        return kakaoPayClient.readyPayment(request);
    }

    private void savePayReadyStatus(PayReadyMessage message, Long pid,
        KakaoPayReadyResponse kakaoPayReadyResponse) {
        Payment payment = Payment.ready(
            message.getOrderId(),
            pid,
            message.getUserId(),
            message.getTotalPrice(),
            kakaoPayReadyResponse.tid()
        );
        paymentRepository.save(payment);
    }

    private void storePaymentUrlInRedis(PayReadyMessage message, KakaoPayReadyResponse response) {
        String orderIdKey = REDIS_KEY_PREFIX + message.getOrderId();
        redisTemplate.opsForValue().set(orderIdKey, response.next_redirect_pc_url());
        redisTemplate.expire(orderIdKey, 10, TimeUnit.MINUTES);
    }

    private KakaoPayApproveResponse requestKakaoPayApprove(PayApproveMessage message,
        Payment payment) {
        ApproveRequest approveRequest = ApproveRequest.builder()
            .cid("TC0ONETIME")
            .tid(payment.getTid())
            .partnerOrderId(String.valueOf(payment.getOrdersId()))
//            .partnerUserId(approvePayment.getUid())
            .partnerUserId(String.valueOf(payment.getUserId()))
            .pgToken(message.getToken())
            .build();

        return kakaoPayClient.paymentApprove(approveRequest);
    }
}
