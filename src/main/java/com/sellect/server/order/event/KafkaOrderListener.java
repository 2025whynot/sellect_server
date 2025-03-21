package com.sellect.server.order.event;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.event.message.OrderCompleteMessage;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.event.message.PayApproveMessage;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaOrderListener {

    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;

    @KafkaListener(topics = "order-complete", groupId = "order-complete-group",
        containerFactory = "orderCompleteContainerFactory")
    @SendTo("order-complete-reply")
    public Boolean orderCompleteListener(OrderCompleteMessage message) {

        Long orderId = message.getPayment().getOrdersId();

        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));

        order.validateNotCompleted();

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(order.getId());

        List<Inventory> deductedInventories = orderItems.stream()
            .map(orderItem -> {
                Inventory inventory = inventoryRepository.findByProductId(orderItem.getProductId())
                    .orElseThrow(() -> new CommonException(BError.NOT_VALID, "product id"));
                return inventory.deductStock(orderItem.getQuantity());

            })
            .toList();

        inventoryRepository.saveAll(deductedInventories);
        ordersRepository.save(order.completeOrder());

        return Boolean.TRUE;
    }

}
