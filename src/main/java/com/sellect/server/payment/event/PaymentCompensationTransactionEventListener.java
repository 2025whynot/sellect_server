package com.sellect.server.payment.event;

import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentCompensationTransactionEventListener {

    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;

    @Async("preparePaymentCompensationExecutor")
    @TransactionalEventListener
    public void handlePreparePaymentFailed(PaymentPrepareFailedEvent event) {
        Orders order = event.getOrders();

        boolean success = false;
        for (int retryCount = 0; retryCount < 2 && !success; retryCount++) {
            try {
                paymentRepository.findByPid(event.getPid())
                    .ifPresent(payment -> paymentRepository.save(payment.failPreparedPayment())); // 존재하면 결제 상태 FAIL로 변경

                ordersRepository.save(order.failOrder()); // 주문 상태 실패로 변경 (PENDING → FAILED)

                success = true;
                log.info("결제 준비 실패 보상 트랜잭션 완료: orderId={}, reason={}", order.getId(), event.getReason());
            } catch (Exception e) {
                log.warn("결제 준비 보상 트랜잭션 재시도: orderId={}, retryCount={}, error={}", order.getId(), retryCount, e.getMessage());
                if (retryCount == 1) { // 최대 1회까지만 시도
                    log.error("결제 준비 보상 트랜잭션 최종 실패: orderId={}, reason={}", order.getId(), event.getReason(), e);
                    break;
                }

                try {
                    Thread.sleep(1000); // 1초 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("결제 준비 보상 트랜잭션 재시도 중 인터럽트: orderId={}", order.getId(), ie);
                    break;
                }
            }
        }
    }
}
