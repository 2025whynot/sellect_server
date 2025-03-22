package com.sellect.server.order.application.v4;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.message.PayApproveMessage;
import com.sellect.server.payment.event.message.PayReadyMessage;
import com.sellect.server.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceV4_0 {

    private static final String REDIS_KEY_PREFIX = "pay-ready:redirect:";

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final KafkaProducer kafkaProducer;
    private final RedisTemplate<String, String> redisTemplate;

    // 주문 결제
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

        // TODO: 멱등성 보장하는지 체크 혹은 보장하지 않아도 상관없는지 체크
        // TODO: 재시도 횟수에 따른 예외 발생 추가
        String key = REDIS_KEY_PREFIX + orderId;
        String redirectUrl = redisTemplate.opsForValue().get(key);
        if (redirectUrl == null) {
            log.debug("No redirect URL found in Redis for key: {}", key);
        } else {
            log.debug("Retrieved redirect URL from Redis: key={}, value={}", key, redirectUrl);
        }

        return redirectUrl;
    }

    // TODO: Transactional 어노테이션 없어도 되는지 체크
    public void approvePayment(final String pid, final String token) {

        Payment payment = paymentRepository.findByPid(pid)
            .orElseThrow(
                () -> new CommonException(BError.NOT_EXIST, String.format("payment %s", pid)));

        userRepository.findById(payment.getUserId())
            .orElseThrow(() -> new CommonException(BError.NOT_VALID, "user id"));

        processPaymentApprovalAsync(payment, pid, token);
    }

    private void processPaymentApprovalAsync(Payment payment, String pid, String token) {
        kafkaProducer.produceWithReply("order-complete", "order-complete-reply", payment.getOrdersId())
            .thenAccept(response -> {
                if (Boolean.TRUE.equals(response)) {
                    log.info("Order complete processed successfully for orderId: {}", payment.getOrdersId());
                    kafkaProducer.produce("pay-approve", PayApproveMessage.builder()
                        .payment(payment)
                        .pid(pid)
                        .token(token)
                        .build());
                }
            })
            .exceptionally(throwable -> {
                log.error("Failed to process payment approval for orderId: {}", payment.getOrdersId(), throwable);
                return null;
            });
    }
}