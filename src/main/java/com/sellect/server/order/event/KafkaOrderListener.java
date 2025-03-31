package com.sellect.server.order.event;

import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.order.application.v4.OrderServiceV4_1;
import com.sellect.server.order.event.message.OrderCompleteMessage;
import com.sellect.server.order.event.message.OrderCompleteRollbackMessage;
import com.sellect.server.order.event.message.StockHistoryMessage;
import com.sellect.server.payment.event.message.PayApproveMessage;
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

    private final OrderServiceV4_1 orderService;
    private final StockHistoryService stockHistoryService;
    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "order-complete", groupId = "order-complete-group",
        containerFactory = "orderCompleteContainerFactory")
    public void orderCompleteListener(OrderCompleteMessage message) {
        consumeOrderCompleteMessage(message);
    }

    @KafkaListener(topics = {"order-complete-failed", "pay-approve-failed"},
        groupId = "order-complete-rollback-group",
        containerFactory = "orderCompleteContainerFactory")
    public void orderCompleteRollbackListener(OrderCompleteRollbackMessage message) {
        consumeOrderCompleteRollbackMessage(message);
    }

    @KafkaListener(topics = "stock-history", groupId = "stock-history-group",
        containerFactory = "stockHistoryContainerFactory")
    public void stockHistoryListener(StockHistoryMessage message) {
        consumeStockHistoryMessage(message);
    }

    // TODO: DLQ 처리 추가
    // === Dead Letter Queue 처리 === //

    // === private method === //

    private void consumeOrderCompleteMessage(OrderCompleteMessage message) {
        try {
            orderService.completeOrder(message.getOrderId());

            log.info("Order complete processed successfully for orderId: {}", message.getOrderId());
            kafkaProducer.produce("pay-approve", PayApproveMessage.builder()
                .payment(message.getPayment())
                .pid(message.getPid())
                .token(message.getToken())
                .build());
        } catch (Exception e) {
            log.error("Failed to process order complete for orderId: {}", message.getOrderId(), e);
            // 보상 트랜잭션 처리
            kafkaProducer.produce("order-complete-failed", OrderCompleteRollbackMessage.builder()
                .orderId(message.getOrderId())
                .pid(message.getPid())
                .build());
        }
    }

    private void consumeOrderCompleteRollbackMessage(OrderCompleteRollbackMessage message) {
        // TODO: v4.0에서의 롤백 추가
        orderService.rollbackOrder(message.getOrderId());
    }

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
