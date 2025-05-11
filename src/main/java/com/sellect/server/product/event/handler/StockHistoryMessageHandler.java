package com.sellect.server.product.event.handler;

import com.sellect.server.order.event.message.StockHistoryMessage;
import com.sellect.server.product.application.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockHistoryMessageHandler {

    private final ProductService productService;

    @KafkaListener(topics = "stock-history",
            groupId = "stock-history-group",
            containerFactory = "stockHistoryContainerFactory")
    public void handleStockHistoryMessage(StockHistoryMessage message) {
        message.getHistoryItems().forEach(historyItem -> productService.updateStock(
                historyItem.getProductId(),
                message.getType().equals("INCREMENT") ? historyItem.getQuantity() : -historyItem.getQuantity())
        );
    }
}
