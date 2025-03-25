package com.sellect.server.payment.application.v1;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.PaymentApproveFailedEvent;
import com.sellect.server.payment.event.PaymentPrepareFailedEvent;
import com.sellect.server.payment.repository.PaymentRepository;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentCompensationServiceV1 {

    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public void compensatePreparePaymentFailed(PaymentPrepareFailedEvent event) {
        Orders order = event.getOrders();

        boolean success = false;
        for (int retryCount = 0; retryCount < 2 && !success; retryCount++) {
            try {
                paymentRepository.findByPid(event.getPid())
                    .ifPresent(payment -> paymentRepository.save(payment.failReady())); // 존재하면 결제 상태 FAIL로 변경

                ordersRepository.save(order.failPending()); // 주문 상태 실패로 변경 (PENDING → FAILED_PENDING)
                success = true;
                log.info("결제 준비 실패 보상 트랜잭션 완료:  orderId={}, pid={}, reason={}", order.getId(), event.getPid(), event.getReason());
            } catch (Exception e) {
                log.warn("결제 준비 보상 트랜잭션 재시도: orderId={}, pid={}, reason={}", order.getId(), event.getPid(), event.getReason(), e);
                if (retryCount == 1) { // 최대 1회까지만 시도
                    log.error("결제 준비 보상 트랜잭션 최종 실패: orderId={}, pid={}, reason={}", order.getId(), event.getPid(), event.getReason(), e);
                    throw new CommonException(BError.COMPENSATION_FAILED, "결제 준비 보상 실패: " + e.getMessage());
                }
                sleepForRetry();
            }
        }
    }

    @Transactional
    public void compensateApprovePaymentFailed(PaymentApproveFailedEvent event) {
        final Payment payment = event.getPayment();

        boolean success = false;

        for (int retryCount = 0; retryCount < 2 && !success; retryCount++) {
            try {
                // 결제 상태 롤백 (APPROVE -> FAIL_APPROVE)
                paymentRepository.save(payment.failApprove());

                // 주문 상태 롤백 (COMPLETED -> FAILED_COMPLETED)
                Orders order = ordersRepository.findByIdWithPessimisticLock(payment.getOrdersId())
                    .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "주문"));
                ordersRepository.save(order.failComplete());

                // 재고 복구 (비관적 락 사용)
                List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(order.getId());

                // todo: 성능 튜닝 지점
                // 데드락 방지 (대신 기아현상 발생 가능성 있음)
                List<OrderItem> sortedOrderItems = orderItems.stream()
                    .sorted(Comparator.comparing(OrderItem::getProductId)) // productId 오름차순 정렬
                    .toList();

                List<Inventory> restoredInventories = sortedOrderItems.stream()
                    .map(orderItem -> {
                        Inventory inventory = inventoryRepository.findWithWriteLockByProductId(orderItem.getProductId())
                            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "재고"));
                        return inventory.restoreStock(orderItem.getQuantity());
                    })
                    .toList();
                inventoryRepository.saveAll(restoredInventories);

                success = true;
                log.info("결제 승인 실패 보상 트랜잭션 완료: pid={}, reason={}", event.getPid(), event.getReason());
            } catch (Exception e) {
                log.warn("결제 승인 보상 트랜잭션 재시도: pid={}, retryCount={}, error={}", event.getPid(), retryCount, e.getMessage());
                if (retryCount == 1) {
                    log.error("결제 승인 보상 트랜잭션 최종 실패: pid={}, reason={}", event.getPid(), event.getReason(), e);
                    throw new CommonException(BError.COMPENSATION_FAILED, "결제 승인 보상 실패: " + e.getMessage());
                }
                sleepForRetry();
            }
        }
    }

    private void sleepForRetry() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CommonException(BError.INTERNAL_SERVER_ERROR, "재시도 중 인터럽트: " + ie.getMessage());
        }
    }
}
