package com.sellect.server.order.event.handler;

import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.order.application.v4.OrderServiceV4;
import com.sellect.server.order.event.message.OrderCompleteFailedMessage;
import com.sellect.server.order.event.message.PaymentApprovalInitMessage;
import com.sellect.server.payment.event.message.OrderCompleteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageHandler {

    private final OrderServiceV4 orderService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "payment-approval-init",
            groupId = "order-complete-group",
            containerFactory = "orderCompleteContainerFactory")
    public void handlePaymentApprovalMessage(PaymentApprovalInitMessage message) {
        try {
            orderService.completeOrder(message.getOrderId());

            kafkaProducer.produce("order-complete", OrderCompleteMessage.builder()
                    .payment(message.getPayment())
                    .pid(message.getPid())
                    .token(message.getToken())
                    .build());
        } catch (Exception e) {
            log.error("Failed to process order complete for orderId: {}", message.getOrderId(), e);

            kafkaProducer.produce("order-complete-failed", OrderCompleteFailedMessage.builder()
                    .orderId(message.getOrderId())
                    .pid(message.getPid())
                    .build());
        }
    }

    @KafkaListener(topics = "pay-approve-failed",
            groupId = "order-complete-failed-group",
            containerFactory = "orderCompleteFailedContainerFactory")
    public void handleOrderCompleteFailedMessage(OrderCompleteFailedMessage message) {
        orderService.processOrderCompletionFailure(message.getOrderId());
    }

    // TODO: DLQ 처리 추가
    // === Dead Letter Queue 처리 === //

}
