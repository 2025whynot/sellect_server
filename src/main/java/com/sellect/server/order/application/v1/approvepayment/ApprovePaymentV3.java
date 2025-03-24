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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovePaymentV3 implements ApprovePaymentStrategy {

    public static final int MAX_RETRIES = 3;

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationEventPublisher eventPublisher;

    // 방법 3. 데드락 발생 시 재시도 로직 추가
    @Override
    public void approvePayment(final Long pid, final String token) {
        int retries = MAX_RETRIES; // 재시도 횟수
        Payment payment = null;

        while (retries > 0) {
            // 트랜잭션 정의 및 시작
            TransactionDefinition definition = new DefaultTransactionDefinition();
            TransactionStatus status = transactionManager.getTransaction(definition);

            Orders order;
            try {
                payment = paymentRepository.findByReadyPid(pid)
                    .orElseThrow(() -> new CommonException(BError.NOT_EXIST,
                        String.format("Payment %s", pid)));

                userRepository.findById(payment.getUserId())
                    .orElseThrow(() -> new CommonException(BError.NOT_VALID, "userId"));

                order = ordersRepository.findByIdWithPessimisticLock(payment.getOrdersId())
                    .orElseThrow(() -> new CommonException(BError.NOT_VALID, "orderId"));

                order.validateNotCompleted();

                List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(order.getId());
                if (orderItems.isEmpty()) {
                    throw new CommonException(BError.NOT_VALID, "orderId");
                }

                List<Inventory> deductedInventories = orderItems.stream()
                    .map(orderItem -> {
                        Inventory inventory = inventoryRepository.findWithWriteLockByProductId(
                                orderItem.getProductId())
                            .orElseThrow(() -> new CommonException(BError.NOT_VALID, "productId"));
                        return inventory.deductStock(orderItem.getQuantity());
                    })
                    .toList();

                inventoryRepository.saveAll(deductedInventories);
                ordersRepository.save(order.completeOrder());
                payment = paymentRepository.save(payment.approve());

                // 트랜잭션 커밋
                transactionManager.commit(status);
                break;
            } catch (CommonException e) {
                transactionManager.rollback(status);
                throw e; // 비즈니스 예외는 재시도 없이 바로 반환
            } catch (Exception e) {
                transactionManager.rollback(status);
                retries--;
                if (retries == 0) {
                    throw new CommonException(BError.INTERNAL_SERVER_ERROR,
                        "approvePayment() - 결제 승인 실패: 최대 재시도 횟수 초과", e.getMessage());
                }
                log.info("결제 승인 재시도 pid: {}, 남은 재시도 횟수: {}", pid, retries);
                try {
                    Thread.sleep(100); // 100ms 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CommonException(BError.INTERNAL_SERVER_ERROR,
                        "approvePayment() - 재시도 중 인터럽트 발생", ie.getMessage());
                }
            }
        }

        if (payment != null) {
            // 트랜잭션 커밋 후 이벤트 발행
            KakaoPayApproveEvent event = KakaoPayApproveEvent.publish(payment, token, pid);
            eventPublisher.publishEvent(event);
        }
    }
}
