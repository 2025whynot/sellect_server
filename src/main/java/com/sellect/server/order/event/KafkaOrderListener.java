package com.sellect.server.order.event;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.redis.RedisTransactionUtil;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.event.message.OrderCompleteMessage;
import com.sellect.server.order.event.message.OrderCompleteReplyMessage;
import com.sellect.server.order.event.message.OrderCompleteFailedMessage;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final RedisTransactionUtil redisTransactionUtil;

    @KafkaListener(topics = "order-complete", groupId = "order-complete-group",
        containerFactory = "orderCompleteContainerFactory")
    @SendTo("order-complete-reply")
    public OrderCompleteReplyMessage orderCompleteListener(OrderCompleteMessage message) {
        return consumeOrderCompleteMessage(message);
    }

    // TODO: consumer group을 order-complete-group으로 해도 되는지 체크
    @KafkaListener(topics = "order-complete-failed", groupId = "order-complete-group",
        containerFactory = "orderCompleteContainerFactory")
    public void payApproveFailedListener(OrderCompleteFailedMessage message) {
        consumeOrderCompleteFailedMessage(message);
    }

    // === Dead Letter Queue 처리 === //


    // === private method === //

    private OrderCompleteReplyMessage consumeOrderCompleteMessage(OrderCompleteMessage message) {

        Orders order = findOrderNotCompleted(message.getPayment().getOrdersId());

        try {
//            processInventories(order); // v4.0
            boolean incremented = incrementStockUsage(order.getId());// v4.1
            if (!incremented) {
                log.warn("Failed to increment stock usage for orderId: {}", order.getId());
                return OrderCompleteReplyMessage.onFail(order.getId());
            }

            saveOrderCompletedStatus(order);
            return OrderCompleteReplyMessage.onComplete(order.getId());
        } catch (Exception e) {
            log.error("Unexpected error in order completion for orderId: {}", order.getId(), e);
            return OrderCompleteReplyMessage.onFail(order.getId());
        }
    }

    private void consumeOrderCompleteFailedMessage(OrderCompleteFailedMessage message) {

        // TODO: v4.0에서의 롤백 추가
        log.info("Processing order-complete-failed for orderId: {}", message.getOrderId());

        // 재고 사용량 롤백
        boolean decremented = decrementStockUsage(message.getOrderId());
        if (!decremented) {
            throw new CommonException(BError.FAIL, "decrement stock usage"); // DLQ에서 처리
        }

        // 주문 상태 롤백
        Orders order = findOrderCompleted(message.getOrderId());
        rollbackOrdersStatus(order);
    }

    private Orders findOrderNotCompleted(Long orderId) {

        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));
        order.validateNotCompleted();
        return order;
    }

    private Orders findOrderCompleted(Long orderId) {

        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));
        order.validateCompleted();
        return order;
    }

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

    // Redis 에서 주문한 상품의 재고 사용량을 증가 (v4.1)
    private boolean incrementStockUsage(final Long orderId) {

        List<OrderItem> orderItems = findOrderItemsSortedByProductId(orderId);

        // 사전 검증
        Map<String, Integer> requestQuantities = new HashMap<>();
        validateBeforeIncrement(orderItems, requestQuantities);

        // 재고 사용량 증가 (Redis 트랜잭션 처리)
        redisTransactionUtil.transaction(operations -> {
            requestQuantities.forEach((stockUsageKey, requestQuantity) -> {
                operations.opsForValue().increment(stockUsageKey, requestQuantity);
            });
        });

        // TODO: RDB에 재고 히스토리 저장
        log.info("Incremented stock usage for orderId: {}", orderId);
        return true;
    }

    private List<OrderItem> findOrderItemsSortedByProductId(Long orderId) {

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        orderItems.sort(Comparator.comparingLong(OrderItem::getProductId));
        return orderItems;
    }


    private void validateBeforeIncrement(List<OrderItem> orderItems, Map<String, Integer> requestQuantities) {

        List<Long> productIds = orderItems.stream()
            .map(OrderItem::getProductId)
            .toList();
        List<Inventory> inventories = findInventoriesSortedByProductId(productIds);

        IntStream.range(0, productIds.size()).forEach(i -> {
            OrderItem orderItem = orderItems.get(i);
            Inventory inventory = inventories.get(i);
            Long productId = productIds.get(i);
            int requestQuantity = orderItem.getQuantity();

            String stockUsageKey = "product:" + productId + ":stock:usage";
            String stockUsageStr = redisTemplate.opsForValue().get(stockUsageKey);
            int stockUsage = stockUsageStr == null ? 0 : Integer.parseInt(stockUsageStr);

            validateStockUsageAndQuantity(inventory.getStock(), requestQuantity, stockUsage, productId);
            requestQuantities.put(stockUsageKey, stockUsage);
        });
    }

    private List<Inventory> findInventoriesSortedByProductId(List<Long> productIds) {

        List<Inventory> inventories = inventoryRepository.findByProductIdsOrderByProductId(
            productIds);
        if (inventories.isEmpty() || inventories.size() != productIds.size()) {
            log.warn("Inventory is not valid for productIds: {}", productIds);
            throw new CommonException(BError.NOT_VALID, "inventory for products " + productIds);
        }
        return inventories;
    }

    private void validateStockUsageAndQuantity(int totalStock, int requestQuantity, int stockUsage, Long productId) {

        if (totalStock < requestQuantity) {
            log.warn("Insufficient stock for productId: {}, requested: {}, available: {}",
                productId, requestQuantity, totalStock);
            throw new CommonException(BError.FAIL_FOR_REASON, "increase stock usage", "insufficient stock");
        }
        if (stockUsage > totalStock) {
            log.warn("Stock usage exceeded total stock for productId: {}, totalUsed: {}, available: {}",
                productId, stockUsage, totalStock);
            throw new CommonException(BError.FAIL_FOR_REASON, "increase stock usage", "stock usage exceeded total stock");
        }
        if (stockUsage + requestQuantity > totalStock) {
            log.warn("Request quantity exceeded total stock for productId: {}, totalUsed: {}, requested: {}",
                productId, stockUsage, requestQuantity);
            throw new CommonException(BError.FAIL_FOR_REASON, "increase stock usage", "request quantity exceeded total stock");
        }
    }

    private boolean decrementStockUsage(final Long orderId) {

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        if (orderItems.isEmpty()) {
            log.warn("No order items found for orderId: {}", orderId);
            return false;
        }

        redisTransactionUtil.transaction(operations -> {
            orderItems.forEach(orderItem -> {
                Long productId = orderItem.getProductId();
                int quantity = orderItem.getQuantity();
                String stockUsageKey = "product:" + productId + ":stock:usage";
                operations.opsForValue().decrement(stockUsageKey, quantity); // TODO: 음수 방지
            });
        });

        log.info("Decremented stock usage for orderId: {}", orderId);
        return true;
    }

    private void saveOrderCompletedStatus(Orders order) {
        ordersRepository.save(order.completeOrder());
    }

    private void rollbackOrdersStatus(Orders order) {
        ordersRepository.save(order.rollbackOrder());
        log.info("Rolled back order status to PENDING for orderId: {}", order.getId());
    }

}
