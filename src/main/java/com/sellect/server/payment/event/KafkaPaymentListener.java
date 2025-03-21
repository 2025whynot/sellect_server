package com.sellect.server.payment.event;

import com.sellect.server.order.Infrastructure.message.PayReadyMessage;
import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaPaymentListener {

    private static final String REDIS_KEY_PREFIX = "pay-ready:redirect:";

    private final KakaoPayClient kakaoPayClient;
    private final PaymentRepository paymentRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(topics = "pay-ready", groupId = "pay-ready-group")
    public void payReadyListener(PayReadyMessage message) {

        System.out.println("message = " + message);
        // 결제 준비 요청
        String pid = UUID.randomUUID().toString();
        KakaoPayReadyRequest request = kakaoPayClient.createKakaoPayReadyRequest(
            String.valueOf(message.getOrderId()),
            String.valueOf(message.getUserId()),
            "test",
            0,
            message.getTotalPrice(),
            pid
        );
        KakaoPayReadyResponse response = kakaoPayClient.readyPayment(request);

        // Payment 저장
        Payment payment = Payment.ready(
            message.getOrderId(),
            pid,
            message.getUserId(),
            message.getTotalPrice(),
            response.tid()
        );
        paymentRepository.save(payment);

        System.out.println("response = " + response);
        storeRedirectUrlInRedis(message, response);

        // TODO: Dead Letter Queue 처리
    }

    private void storeRedirectUrlInRedis(PayReadyMessage message, KakaoPayReadyResponse response) {
        String orderIdKey = REDIS_KEY_PREFIX + message.getOrderId();
        redisTemplate.opsForValue().set(orderIdKey, response.next_redirect_pc_url());
        redisTemplate.expire(orderIdKey, 1, TimeUnit.HOURS);
    }
}
