package com.sellect.server.order.application.v1.approvepayment;

import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.application.v1.approvepayment.v5.RedisStockService;
import com.sellect.server.order.application.v1.approvepayment.v5.StockDeductionResult;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.KakaoPayApproveRedisEvent;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovePaymentV6 implements ApprovePaymentStrategy {

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisStockService redisStockService;
    private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 추가

    private static final String PAYMENT_LOCK_PREFIX = "payment:lock:pid:";

    @Override
    public void approvePayment(final Long pid, final String token) {
        // Payment 조회
        Payment payment = paymentRepository.findByReadyPid(pid)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, String.format("Payment %s", pid)));

        // 사용자 검증
        userRepository.findById(payment.getUserId())
            .orElseThrow(() -> new CommonException(BError.NOT_VALID, "userId"));

        // 중복 결제 방지: Redis에 pid 잠금 설정 (TTL 5초)
        String lockKey = PAYMENT_LOCK_PREFIX + pid;
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED");
        if (Boolean.FALSE.equals(isLocked)) {
            throw new CommonException(BError.DUPLICATE, "Duplicate payment attempt for pid: " + pid);
        }
        redisTemplate.expire(lockKey, 5, TimeUnit.SECONDS); // TTL 5초 설정

        try {
            // 주문 조회 및 검증
            Orders order = ordersRepository.findById(payment.getOrdersId())
                .orElseThrow(() -> new CommonException(BError.NOT_VALID, "orderId"));
            order.validateNotCompleted();

            List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(order.getId());
            if (orderItems.isEmpty()) {
                throw new CommonException(BError.NOT_VALID, "orderId");
            }

            // 재고 차감 (Lua 스크립트로 원자성 보장)
            StockDeductionResult result = redisStockService.tryDeductStocks(orderItems);
            if (!result.isSuccess()) {
                throw new CommonException(BError.OUT_OF_STOCK, "Insufficient stock");
            }

            // DB 트랜잭션
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            try {
                ordersRepository.save(order.completeOrder());
                transactionManager.commit(status);
            } catch (Exception e) {
                transactionManager.rollback(status);
                redisStockService.rollbackStocks(result.getDeductedStocks());
                throw new CommonException(BError.INTERNAL_SERVER_ERROR, "approvePayment() - 결제 승인 중 오류 발생");
            }

            // 이벤트 발행
            eventPublisher.publishEvent(
                KakaoPayApproveRedisEvent.publish(orderItems, payment, token, pid));
        } finally {
            // 결제 처리 완료 후 Redis 락 해제
            redisTemplate.delete(lockKey);
        }
    }
}