package com.sellect.server.payment.event;

import com.github.f4b6a3.tsid.TsidCreator;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.request.KakaoPayReadyRequest;
import com.sellect.server.order.Infrastructure.response.KakaoPayApproveResponse;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.application.PaymentService;
import com.sellect.server.payment.controller.request.ApproveRequest;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.message.PayApproveMessage;
import com.sellect.server.payment.event.message.PayApproveRollbackMessage;
import com.sellect.server.payment.event.message.PayReadyMessage;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
        paymentService.preparePayment(message.getOrderId(), message.getUserId(), message.getTotalPrice());
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

    }
}
