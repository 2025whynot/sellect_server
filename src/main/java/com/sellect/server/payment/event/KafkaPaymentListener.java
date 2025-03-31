package com.sellect.server.payment.event;

import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.payment.application.PaymentService;
import com.sellect.server.payment.event.message.PayApproveMessage;
import com.sellect.server.payment.event.message.PayApproveRollbackMessage;
import com.sellect.server.payment.event.message.PayReadyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentListener {

    private final KafkaProducer kafkaProducer;
    private final PaymentService paymentService;


    @KafkaListener(topics = "pay-ready", groupId = "pay-ready-group")
    public void payReadyListener(PayReadyMessage message) {
        log.info("Received pay-ready message: {}", message);
        consumePayReadyMessage(message);
    }

    @KafkaListener(topics = "pay-approve", groupId = "pay-approve-group")
    public void payApproveListener(PayApproveMessage message) {
        consumePayApproveMessage(message);
    }

    @KafkaListener(topics = "pay-approve-failed", groupId = "pay-approve-group")
    public void payApproveFailedListener(PayApproveRollbackMessage message) {
        consumePayApproveRollbackMessage(message);
    }

    // TODO: DLQ 처리 추가
    // === Dead Letter Queue 처리 === //


    // === private method === //

    private void consumePayReadyMessage(PayReadyMessage message) {
        log.info("Starting to process pay-ready message: userId={}, orderId={}, totalPrice={}",
            message.getUserId(), message.getOrderId(), message.getTotalPrice());
        paymentService.preparePayment(message.getUserId(), message.getOrderId(), message.getTotalPrice());
        log.info("Successfully processed pay-ready message: {}", message);
    }

    private void consumePayApproveMessage(PayApproveMessage message) {
        try {
            paymentService.approvePayment(message.getPid(), message.getToken());
        } catch (Exception e) {
            // 보상 트랜잭션
            kafkaProducer.produce("pay-approve-failed", message);
        }
    }

    private void consumePayApproveRollbackMessage(PayApproveRollbackMessage message) {
        paymentService.rollbackPayment(message.getPid());
    }
}
