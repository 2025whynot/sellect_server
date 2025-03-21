package com.sellect.server.order.application.v4;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.order.Infrastructure.message.PayReadyMessage;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV4 {

    private static final String REDIS_KEY_PREFIX = "pay-ready:redirect:";

    private final OrdersRepository ordersRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final KafkaProducer kafkaProducer;
    private final RedisTemplate<String, String> redisTemplate;

    // 주문 결제
    @Transactional
    public void preparePayment(User user, Long orderId, Long userReceivedCouponId) {
        // 주문 조회
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));

        // 유저의 주문인지 확인
        order.validateOwner(user);

        // 쿠폰 적용
        if (userReceivedCouponId != null) {
            UserReceivedCoupon coupon = userReceivedCouponRepository.findById(userReceivedCouponId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "쿠폰"));
            order = ordersRepository.save(order.applyCoupon(coupon));
        }

        PayReadyMessage message = PayReadyMessage.builder()
            .userId(user.getId())
            .orderId(orderId)
            .totalPrice(order.getTotalPrice().intValue())
            .build();
        kafkaProducer.produce("pay-ready", message);
    }

    public String getPaymentUrl(User user, Long orderId) {
        // TODO: 유저 검증
        String key = REDIS_KEY_PREFIX + orderId;
        try {
            String redirectUrl = redisTemplate.opsForValue().get(key);
            if (redirectUrl == null) {
                log.debug("No redirect URL found in Redis for key: {}", key);
            } else {
                log.debug("Retrieved redirect URL from Redis: key={}, value={}", key, redirectUrl);
            }
            return redirectUrl;
        } catch (Exception e) {
            log.error("Failed to retrieve redirect URL from Redis for key: {}", key, e);
            throw new RuntimeException("Redis retrieve operation failed", e);
        }
    }
}