package com.sellect.server.order.event;

import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.order.application.v4.OrderServiceV4;
import com.sellect.server.order.event.message.PaymentApprovalInitMessage;
import com.sellect.server.order.event.message.OrderCompleteFailedMessage;
import com.sellect.server.order.event.message.StockHistoryMessage;
import com.sellect.server.payment.event.message.OrderCompleteMessage;
import com.sellect.server.product.application.StockHistoryService;
import com.sellect.server.product.repository.StockHistoryEntity;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderListener {

    private final OrderServiceV4 orderService;
    private final StockHistoryService stockHistoryService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "payment-approval-init",
            groupId = "order-complete-group",
            containerFactory = "orderCompleteContainerFactory")
    public void consumePaymentApprovalMessage(PaymentApprovalInitMessage message) {
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

    @KafkaListener(topics = {"order-complete-failed", "pay-approve-failed"},
            groupId = "order-complete-failed-group",
            containerFactory = "orderCompleteFailedContainerFactory")
    public void consumeOrderCompleteFailedMessage(OrderCompleteFailedMessage message) {
        orderService.rollbackOrder(message.getOrderId());
    }

    @KafkaListener(topics = "stock-history", groupId = "stock-history-group",
            containerFactory = "stockHistoryContainerFactory")
    public void stockHistoryListener(StockHistoryMessage message) {
        consumeStockHistoryMessage(message);
    }

    // TODO: DLQ 처리 추가
    // === Dead Letter Queue 처리 === //

    // === private method === //

    private void consumeStockHistoryMessage(StockHistoryMessage message) {

        List<StockHistoryEntity> stockHistoryEntities = message.getHistoryItems().stream()
                .map(item -> StockHistoryEntity.builder()
                        .userId(message.getUserId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .type(message.getType())
                        .createdAt(message.getCreatedAt())
                        .build())
                .toList();
        stockHistoryService.saveAllStockHistory(stockHistoryEntities);
    }

}
