package com.sellect.server.order.application.v4;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.event.message.OrderCompleteMessage;
import com.sellect.server.order.event.message.OrderCompleteReplyMessage;
import com.sellect.server.order.event.message.OrderCompleteFailedMessage;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.message.PayApproveMessage;
import com.sellect.server.payment.event.message.PayReadyMessage;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV4_1 { // v4.0에서 Redis로 재고 관리하는 것만 추가

    private static final String REDIS_KEY_PREFIX = "pay-ready:redirect:";
    private static final String RETRY_KEY_PREFIX = "pay-ready:retry-count:";
    private static final int MAX_RETRY_COUNT = 5;

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final KafkaProducer kafkaProducer;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void preparePayment(User user, final Long orderId, final Long userReceivedCouponId) {

        // 주문 조회
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));

        // 유저의 주문인지 확인
        order.validateOwner(user);

        // 쿠폰 적용
        if (userReceivedCouponId != null) {
            UserReceivedCoupon coupon = userReceivedCouponRepository.findById(userReceivedCouponId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "coupon"));
            order = ordersRepository.save(order.applyCoupon(coupon));
        }

        PayReadyMessage message = PayReadyMessage.builder()
            .userId(user.getId())
            .orderId(orderId)
            .totalPrice(order.getTotalPrice().intValue())
            .build();
        kafkaProducer.produce("pay-ready", message);
    }

    public String getPaymentUrl(User user, final Long orderId) {

        // 주문 조회
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));

        // 유저의 주문인지 확인
        order.validateOwner(user);

        return getRedirectUrlFromRedis(user.getId(), orderId);
    }

    public void approvePayment(final String pid, final String token) {

        Payment payment = paymentRepository.findByPid(pid)
            .orElseThrow(
                () -> new CommonException(BError.NOT_EXIST, String.format("payment %s", pid)));

        userRepository.findById(payment.getUserId())
            .orElseThrow(() -> new CommonException(BError.NOT_VALID, "user id"));

        processPaymentApproval(payment, pid, token);
    }

    //== private methods ==//

    private String getRedirectUrlFromRedis(Long userId, Long orderId) {
        String redirectUrlKey = REDIS_KEY_PREFIX + orderId;
        String retryCountKey = RETRY_KEY_PREFIX + userId + ":" + orderId;

        // 재시도 횟수 체크 및 증가 (원자적 연산)
        Long retryCount = redisTemplate.opsForValue().increment(retryCountKey, 1L);
        if (retryCount == null) retryCount = 1L; // 초기 값 처리

        if (retryCount > MAX_RETRY_COUNT) {
            log.warn("Max retry count exceeded for userId: {}, orderId: {}", userId, orderId);
            throw new CommonException(BError.FAIL_FOR_REASON, "get redirect URL from Redis",
                "Max retry count exceeded");
        }

        // TTL 설정
        if (retryCount == 1) {
            redisTemplate.expire(retryCountKey, 10, TimeUnit.MINUTES);
        }

        // Redirect URL 조회
        String redirectUrl = redisTemplate.opsForValue().get(redirectUrlKey);
        if (redirectUrl == null) {
            log.debug("No redirect URL found in Redis for key: {}", redirectUrlKey);
        } else {
            log.debug("Retrieved redirect URL from Redis: key={}, value={}", redirectUrlKey, redirectUrl);
        }
        return redirectUrl;
    }

    private void processPaymentApproval(Payment payment, String pid, String token) {
        kafkaProducer.produceWithReply("order-complete", "order-complete-reply",
                OrderCompleteMessage.builder().payment(payment))
            .thenAccept(reply -> {
                OrderCompleteReplyMessage replyMessage = (OrderCompleteReplyMessage) reply;

                if (Boolean.TRUE.equals(replyMessage.getOrderCompleted())) {
                    log.info("Order complete processed successfully for orderId: {}", replyMessage.getOrderId());
                    kafkaProducer.produce("pay-approve", PayApproveMessage.builder()
                        .payment(payment)
                        .pid(pid)
                        .token(token)
                        .build());
                } else {
                    log.warn("Order complete failed for orderId: {}", replyMessage.getOrderId());
                    kafkaProducer.produce("order-complete-failed", OrderCompleteFailedMessage.builder()
                        .orderId(payment.getOrdersId())
                        .pid(pid)
                        .build());
                }
            })
            .exceptionally(throwable -> {
                log.error("Failed to process payment approval for orderId: {}", payment.getOrdersId(), throwable);
                return null;
            });
    }
}