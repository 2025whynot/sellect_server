package com.sellect.server.order.event.handler;

import com.sellect.server.order.application.v4.OrderServiceV4;
import com.sellect.server.order.event.message.OrderCompleteFailedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageHandler {

    private final OrderServiceV4 orderService;

    @KafkaListener(topics = "order-complete-failed",
            groupId = "order-complete-failed-group",
            containerFactory = "orderCompleteFailedContainerFactory")
    public void consumeOrderCompleteFailedMessage(OrderCompleteFailedMessage message) {
        orderService.processOrderCompletionFailure(message.getOrderId());
    }
}
