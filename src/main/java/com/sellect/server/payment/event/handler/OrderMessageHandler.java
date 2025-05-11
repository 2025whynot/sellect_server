package com.sellect.server.payment.event.handler;

import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.payment.application.PaymentService;
import com.sellect.server.payment.event.message.OrderCompleteMessage;
import com.sellect.server.payment.event.message.OrderReadyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageHandler {

    private final KafkaProducer kafkaProducer;
    private final PaymentService paymentService;

    @KafkaListener(topics = "order-ready", groupId = "pay-ready-group")
    public void handleOrderReadyMessage(OrderReadyMessage message) {
        paymentService.preparePayment(message.getUserId(), message.getOrderId(), message.getTotalPrice());
    }

    @KafkaListener(topics = "order-complete", groupId = "pay-approve-group")
    public void handleOrderCompleteMessage(OrderCompleteMessage message) {
        try {
            paymentService.approvePayment(message.getPid(), message.getToken());
        } catch (Exception e) {
            log.error("Failed to process payment approval for pid: {}", message.getPid(), e);

            kafkaProducer.produce("pay-approve-failed", message);
        }
    }

    // TODO: DLQ 처리 추가
    // === Dead Letter Queue 처리 === //
}
