package com.sellect.server.payment.event.handler;

import com.sellect.server.payment.application.PaymentService;
import com.sellect.server.payment.event.message.PayApproveFailedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMessageHandler {

    private final PaymentService paymentService;

    @KafkaListener(topics = "pay-approve-failed", groupId = "pay-approve-failed-group")
    public void handlePayApproveFailedMessage(PayApproveFailedMessage message) {
        paymentService.processPaymentApprovalFailure(message.getPid());
    }
}
