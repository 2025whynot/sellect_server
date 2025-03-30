package com.sellect.server.order.application.v1.approvepayment;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.application.v1.approvepayment.v5.RedisStockService;
import com.sellect.server.order.application.v1.approvepayment.v5.StockDeductionResult;
import com.sellect.server.order.application.v1.approvepayment.v5.StockSyncService;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
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
    private final StockSyncService stockSyncService;
    private final RedissonClient redissonClient;
    private final RedisStockService redisStockService;

    private static final String APPROVE_PAYMENT_LOCK_KEY = "lock:approvePayment";

    // 방법 5. 레디스를 분산락으로 제어, 재고 차감은 Redis 사용
    @Override
    public void approvePayment(final Long pid, final String token) {
        List<OrderItem> orderItems;

        Payment payment = paymentRepository.findByReadyPid(pid)
            .orElseThrow(
                () -> new CommonException(BError.NOT_EXIST, String.format("Payment %s", pid)));

        userRepository.findById(payment.getUserId())
            .orElseThrow(() -> new CommonException(BError.NOT_VALID, "userId"));
        // pid별 락으로 중복 결제 방지
        RLock pidLock = redissonClient.getLock(APPROVE_PAYMENT_LOCK_KEY);
        try {
            // 락 획득 (최대 2초 대기, 2초 TTL) -> 성능 테스트용으로는 (waitTime : 10, leaseTime : 5)로 예정
            if (!pidLock.tryLock(2, 2, TimeUnit.SECONDS)) {
                throw new CommonException(BError.TIMEOUT, "Failed to acquire lock");
            }

            Orders order = ordersRepository.findById(payment.getOrdersId())
                .orElseThrow(() -> new CommonException(BError.NOT_VALID, "orderId"));
            order.validateNotCompleted();

            orderItems = orderItemRepository.findAllByOrdersId(order.getId());
            if (orderItems.isEmpty()) {
                throw new CommonException(BError.NOT_VALID, "orderId");
            }

            stockSyncService.preloadStocksIfNeeded(orderItems);

            StockDeductionResult result = redisStockService.tryDeductStocks(orderItems);
            if (!result.isSuccess()) {
                throw new CommonException(BError.OUT_OF_STOCK, "Insufficient stock");
            }

            // 트랜잭션 시작
            TransactionStatus status = transactionManager.getTransaction(
                new DefaultTransactionDefinition());
            try {
                ordersRepository.save(order.completeOrder());
                transactionManager.commit(status); // 트랜잭션 커밋
            } catch (Exception e) {
                transactionManager.rollback(status);
                // DB 실패 시 Redis 재고 복구
                redisStockService.rollbackStocks(result.getDeductedStocks());
                throw new CommonException(BError.INTERNAL_SERVER_ERROR,
                    "approvePayment() - 결제 승인 중 오류 발생");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
            throw new CommonException(BError.INTERNAL_SERVER_ERROR,
                "Interrupted while acquiring lock");
        } finally {
            if (pidLock.isHeldByCurrentThread()) {
                pidLock.unlock();
            }
        }

        // 트랜잭션 커밋 후 이벤트 발행
        eventPublisher.publishEvent(
            KakaoPayApproveRedisEvent.publish(orderItems, payment, token, pid));
    }

    private Long generatePid() {
        return TsidCreator.getTsid().toLong();
    }
}