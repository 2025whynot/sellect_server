package com.sellect.server.payment.event.listener;

import com.sellect.server.payment.application.PaymentCompensationServiceV1;
import com.sellect.server.payment.event.PaymentApproveFailedEvent;
import com.sellect.server.payment.event.PaymentPrepareFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentCompensationTransactionEventListener {

    private final PaymentCompensationServiceV1 paymentCompensationService;

    @Async("preparePaymentCompensationExecutor")
    @TransactionalEventListener
    public void handlePreparePaymentFailed(PaymentPrepareFailedEvent event) {
        log.info("결제 준비 실패 이벤트 수신: orderId={}", event.getOrders().getId());
        paymentCompensationService.compensatePreparePaymentFailed(event);
    }

    @Async("approvePaymentCompensationExecutor")
    @TransactionalEventListener
    public void handleApprovePaymentFailed(PaymentApproveFailedEvent event) {
        log.info("결제 승인 실패 이벤트 수신: orderId={}", event.getPayment().getOrdersId());
        paymentCompensationService.compensateApprovePaymentFailed(event);
    }
}
