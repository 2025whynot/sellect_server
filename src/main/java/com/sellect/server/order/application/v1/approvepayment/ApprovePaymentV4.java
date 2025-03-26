package com.sellect.server.order.application.v1.approvepayment;

import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.KakaoPayApproveEvent;
import com.sellect.server.payment.repository.PaymentRepository;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
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
public class ApprovePaymentV4 implements ApprovePaymentStrategy {

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;

    // 방법 4. 레디스를 분산락으로 제어, 재고 차감은 MySQL 그대로 이용 (다만, 비관적 락을 레디스 분산락으로 변경)
    @Override
    public void approvePayment(final Long pid, final String token) {

        Payment payment = paymentRepository.findByReadyPid(pid)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, String.format("Payment %s", pid)));
        userRepository.findById(payment.getUserId())
            .orElseThrow(() -> new CommonException(BError.NOT_VALID, "userId"));

        // 분산 락을 pid를 기반으로 설정 (중복 결제 방지)
        RLock lock = redissonClient.getLock("lock:approvePayment");

        try {
            // 락 획득 (최대 2초 대기, 2초 TTL) -> 성능 테스트용으로는 (waitTime : 10, leaseTime : 5)로 예정
            if (!lock.tryLock(2, 2, TimeUnit.SECONDS)) {
                throw new CommonException(BError.TIMEOUT, "Failed to acquire lock");
            }

            // 트랜잭션 시작
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

            try {
                Orders order = ordersRepository.findById(payment.getOrdersId())
                    .orElseThrow(() -> new CommonException(BError.NOT_VALID, "orderId"));

                order.validateNotCompleted();

                List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(order.getId());
                if (orderItems.isEmpty()) { // 서비스에 위임
                    throw new CommonException(BError.NOT_VALID, "orderId");
                }

                List<Inventory> deductedInventories = orderItems.stream()
                    .map(orderItem -> {
                        Inventory inventory = inventoryRepository.findByProductId(
                                orderItem.getProductId())
                            .orElseThrow(() -> new CommonException(BError.NOT_VALID, "productId"));
                        return inventory.deductStock(orderItem.getQuantity());
                    })
                    .toList();

                inventoryRepository.saveAll(deductedInventories);
                ordersRepository.save(order.completeOrder());

                // 트랜잭션 커밋
                transactionManager.commit(status);
            } catch (CommonException e) {
                // 예외 발생 시 롤백
                transactionManager.rollback(status);
                throw e;
            } catch (Exception e) {
                transactionManager.rollback(status);
                throw new CommonException(BError.INTERNAL_SERVER_ERROR, "approvePayment() - 결제 승인 중 오류 발생");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
            throw new CommonException(BError.INTERNAL_SERVER_ERROR, "Interrupted while acquiring lock");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // 트랜잭션 커밋 후 이벤트 발행
        KakaoPayApproveEvent event = KakaoPayApproveEvent.publish(payment, token, pid);
        eventPublisher.publishEvent(event);
    }
}
