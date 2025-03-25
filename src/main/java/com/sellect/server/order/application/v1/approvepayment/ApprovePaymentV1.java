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
import java.util.Comparator;
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
public class ApprovePaymentV1 implements ApprovePaymentStrategy {

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationEventPublisher eventPublisher;

    // 방법 1. 데드락에 대한 회피 방식 (정렬) -> 기아현상 발생
    @Override
    public void approvePayment(final Long pid, final String token) {

        // 트랜잭션 정의 및 시작
        TransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(definition);

        Payment payment;
        Orders order;

        try {
            payment = paymentRepository.findByReadyPid(pid)
                .orElseThrow(() -> new CommonException(
                    BError.NOT_EXIST, String.format("Payment %s", pid)));

            userRepository.findById(payment.getUserId())
                .orElseThrow(() -> new CommonException(BError.NOT_VALID, "userId"));

            // todo: 추론: 낙관 (이유는 중복 결제가 현재 자주 발생하지 않을 것이라고 예상)
            order = ordersRepository.findByIdWithPessimisticLock(payment.getOrdersId())
                .orElseThrow(() -> new CommonException(BError.NOT_VALID, "orderId"));

            order.validateNotCompleted();

            List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(order.getId());
            if (orderItems.isEmpty()) { // 서비스에 위임
                throw new CommonException(BError.NOT_VALID, "orderId");
            }

            // 해결법 (1) - 데드락 회피 -> 예상 : 기아현상 발생
            List<OrderItem> sortedOrderItems = orderItems.stream()
                .sorted(Comparator.comparing(OrderItem::getProductId)) // productId 오름차순 정렬
                .toList();

            List<Inventory> deductedInventories = sortedOrderItems.stream()
                .map(orderItem -> {
                    Inventory inventory = inventoryRepository.findWithWriteLockByProductId(
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

        // 트랜잭션 커밋 후 이벤트 발행
        KakaoPayApproveEvent event = KakaoPayApproveEvent.publish(payment, token, pid);
        eventPublisher.publishEvent(event);
    }
}
