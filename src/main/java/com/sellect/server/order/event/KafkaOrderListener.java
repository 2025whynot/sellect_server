package com.sellect.server.order.event;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.event.message.OrderCompleteMessage;
import com.sellect.server.order.event.message.OrderCompleteReplyMessage;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderListener {

    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // TODO: true/false가 반환되는 경우와 예외가 발생되는 경우 각 상황에 맞게 후처리되는지 확인
    @KafkaListener(topics = "order-complete", groupId = "order-complete-group",
        containerFactory = "orderCompleteContainerFactory")
    @SendTo("order-complete-reply")
    public OrderCompleteReplyMessage orderCompleteListener(OrderCompleteMessage message) {
        return consumeOrderCompleteMessage(message);
    }

    // === private method === //

    private OrderCompleteReplyMessage consumeOrderCompleteMessage(OrderCompleteMessage message) {

        Orders order = findOrder(message);
        order.validateNotCompleted();

//        processInventories(order); // v4.0
        boolean increased = increaseStockUsage(order.getId()); // v4.1
        if (!increased) {
            log.warn("Failed to increase stock usage for orderId: {}", order.getId());
            return OrderCompleteReplyMessage.onFail(order.getId());
        }

        saveOrderCompletedStatus(order);
        return OrderCompleteReplyMessage.onComplete(order.getId());
    }

    private Orders findOrder(OrderCompleteMessage message) {
        Long orderId = message.getPayment().getOrdersId();
        return ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));
    }

    // TODO: pay-approve가 실패하는 이벤트에 대한 리스너 추가
    //  1. inventory rollback (v4.0)
    //  2. 재고 사용량 감소 (v4.1)

    // inventory 테이블에서 주문한 상품의 재고를 차감 (v4.0)
    private void processInventories(Orders order) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(order.getId());

        List<Inventory> processedInventories = orderItems.stream()
            .map(orderItem -> {
                Inventory inventory = inventoryRepository.findByProductId(orderItem.getProductId())
                    .orElseThrow(() -> new CommonException(BError.NOT_VALID, "product id"));
                return inventory.deductStock(orderItem.getQuantity());
            })
            .toList();
        inventoryRepository.saveAll(processedInventories);
    }

    // Redis에서 주문한 상품의 재고 사용량을 증가 (v4.1)
    public boolean increaseStockUsage(final Long orderId) {

        // TODO: 리팩토링 필요
        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        orderItems.sort(Comparator.comparingLong(OrderItem::getProductId));

        List<Long> productIds = orderItems.stream()
            .map(OrderItem::getProductId)
            .toList();

        List<Inventory> inventories = inventoryRepository.findByProductIdsOrderByProductId(productIds);
        if (inventories.isEmpty()) {
            log.warn("Inventory not found for productIds: {}", productIds);
            return false;
        }
        if (inventories.size() != productIds.size()) {
            log.warn("Inventory not found for some productIds: {}", productIds);
            return false;
        }

        IntStream.range(0, productIds.size()).forEach(i -> {
            int quantity = orderItems.get(i).getQuantity();
            Inventory inventory = inventories.get(i);
            Long productId = productIds.get(i);

            if (inventory.getStock() < quantity) {
                log.warn("Insufficient stock for productId: {}, requested: {}, available: {}",
                    productId, quantity, inventory.getStock());
                throw new CommonException(BError.FAIL_FOR_REASON, "increase stock usage",
                    "insufficient stock");
            }

            String stockUsageKey = "product:" + productId + ":stock:usage";
            Long stockUsage = redisTemplate.opsForSet().size(stockUsageKey);
            stockUsage = stockUsage != null ? stockUsage : 0L;

            if (stockUsage + quantity > inventory.getStock()) {
                log.warn("Total stock exceeded for productId: {}, totalUsed: {}, requested: {}",
                    productId, stockUsage, quantity);
                throw new CommonException(BError.FAIL_FOR_REASON, "increase stock usage",
                    "request quantity exceeded total stock");
            }

            redisTemplate.opsForSet().add(stockUsageKey, orderId.toString());
        });

        log.info("increased stock usage for productId: {}, orderId: {}", productIds, orderId);
        return true;
    }

    private void saveOrderCompletedStatus(Orders order) {
        ordersRepository.save(order.completeOrder());
    }

}
