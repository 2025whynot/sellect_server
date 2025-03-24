package com.sellect.server.payment.event.listener;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.application.PaymentServiceV1;
import com.sellect.server.payment.event.KakaoPayApproveEvent;
import com.sellect.server.payment.event.KakaoPayReadyEvent;
import com.sellect.server.payment.event.PaymentApproveFailedEvent;
import com.sellect.server.payment.event.PaymentPrepareFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventListener {

    private final PaymentServiceV1 paymentService;
    private final ApplicationEventPublisher eventPublisher;

    @Async("preparePaymentExecutor")
    @EventListener
    public void kakaoPayReadyEvent(KakaoPayReadyEvent event) {
        try {
            KakaoPayReadyResponse response = paymentService.preparePayment(event);
            event.getFuture().complete(response);
        } catch (CommonException e) {
            event.getFuture().completeExceptionally(e);
            eventPublisher.publishEvent(
                PaymentPrepareFailedEvent.builder()
                    .orders(event.getOrders())
                    .pid(null) // pid 가 PaymentService 내부에서 생성되므로 여기서는 null로 처리
                    .reason(e.getMessage())
                    .build()
            );
        }
    }

    @Async("approvePaymentExecutor")
    @EventListener
    public void kakaoPayApproveEvent(final KakaoPayApproveEvent event) {

        try {
            paymentService.approvePayment(event);
        } catch (Exception e) {
            log.error("카카오페이 승인 실패: pid={}, error={}", event.getPid(), e.getMessage());
            eventPublisher.publishEvent(
                PaymentApproveFailedEvent.builder()
                    .payment(event.getPayment())
                    .pid(event.getPid())
                    .reason(e.getMessage())
                    .build()
            );
        }
    }
}
