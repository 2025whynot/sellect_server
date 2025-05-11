package com.sellect.server.order.application.v4;

import com.sellect.server.auth.domain.User;
import com.sellect.server.auth.repository.user.UserRepository;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.kafka.KafkaProducer;
import com.sellect.server.common.redis.RedisTransactionUtil;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.order.domain.Orders;
import com.sellect.server.order.event.message.StockHistoryMessage;
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.event.message.OrderReadyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceV4 {

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTransactionUtil redisTransactionUtil;
    private final KafkaProducer kafkaProducer;

    @Transactional
    public void prepareOrder(Long userId, final Long orderId, final Long userReceivedCouponId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user"));
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));

        order.validateOwner(user);

        if (userReceivedCouponId != null) {
            UserReceivedCoupon coupon = userReceivedCouponRepository.findById(userReceivedCouponId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "coupon"));
            order = ordersRepository.save(order.applyCoupon(coupon));
        }

        kafkaProducer.produce("order-ready", OrderReadyMessage.builder()
            .orderId(order.getId())
            .userId(order.getUser().getId())
            .totalPrice(order.getTotalPrice().intValue())
            .build());
    }

    @Transactional
    public void completeOrder(final Long orderId) {
        Orders order = findOrderNotCompleted(orderId);

        try {
            decrementStockQuantity(orderId);
        } catch (Exception e) {
            log.error("Failed to decrement stock quantity for orderId: {}", orderId, e);
            return;
        }

        try {
            saveOrderCompletedStatus(order);
        } catch (Exception e) {
            log.error("Unexpected error in order completion for orderId: {}", orderId, e);
            throw new CommonException(BError.FAIL_FOR_REASON, "complete order", "unexpected error");
        }
    }

    @Transactional
    public void processOrderCompletionFailure(Long orderId) {
        log.info("Processing order-complete-failed for orderId: {}", orderId);

        // 재고 수량 복구
        incrementStockQuantity(orderId);

        // 주문 상태: COMPLETED -> FAILED_COMPLETED
        Orders order = findOrderCompleted(orderId);
        saveOrderFailedCompletedStatus(order);
    }

    //== private methods ==//

    private Orders findOrderNotCompleted(Long orderId) {

        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));
        order.validateNotCompleted();
        return order;
    }

    // Redis에서 주문한 상품의 남은 재고 수량을 감소
    private void decrementStockQuantity(final Long orderId) {

        List<OrderItem> orderItems = findOrderItemsSortedByProductId(orderId);
        Map<String, Integer> requestQuantities = getRequestQuantities(orderItems);

        // 재고 수량 감소 (Redis 트랜잭션 처리)
        redisTransactionUtil.transaction(operations -> {
            requestQuantities.forEach((stockKey, requestQuantity) -> {
                operations.opsForValue().decrement(stockKey, requestQuantity);
            });
        });

        log.debug("Decremented stock quantity for orderId: {}", orderId);

        // 재고 히스토리 이벤트 전송
        kafkaProducer.produce("stock-history", StockHistoryMessage.builder()
            .userId(orderItems.get(0).getOrders().getUser().getId())
            .type("DECREMENT")
            .createdAt(LocalDateTime.now())
            .historyItems(orderItems.stream()
                .map(orderItem -> StockHistoryMessage.HistoryItem.builder()
                    .productId(orderItem.getProductId())
                    .quantity(orderItem.getQuantity())
                    .build())
                .toList())
            .build());
    }

    private List<OrderItem> findOrderItemsSortedByProductId(Long orderId) {

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        return orderItems.stream()
            .sorted(Comparator.comparingLong(OrderItem::getProductId))
            .toList();
    }


    private Map<String, Integer> getRequestQuantities(List<OrderItem> orderItems) {

        Map<String, Integer> requestQuantities = new HashMap<>();

        orderItems.forEach(orderItem -> {
            int requestQuantity = orderItem.getQuantity();

            String stockKey = "product:" + orderItem.getProductId() + ":stock";
            String stockQuantityStr = redisTemplate.opsForValue().get(stockKey);
            int stockQuantity = stockQuantityStr == null ? 0 : Integer.parseInt(stockQuantityStr);

            validateQuantity(requestQuantity, stockQuantity, orderItem.getProductId());
            requestQuantities.put(stockKey, requestQuantity);
        });

        return requestQuantities;
    }

    private void validateQuantity(int requestQuantity, int stockQuantity, Long productId) {

        if (stockQuantity <= 0) {
            log.warn("Stock quantity is zero or negative for productId: {}", productId);
            throw new CommonException(BError.FAIL_FOR_REASON, "decrement stock quantity",
                "stock quantity is zero or negative");
        }

        if (stockQuantity < requestQuantity) {
            log.warn("Insufficient stock for productId: {}, requested: {}, available: {}",
                productId, requestQuantity, stockQuantity);
            throw new CommonException(BError.FAIL_FOR_REASON, "decrement stock quantity",
                "insufficient stock");
        }
    }

    private Orders findOrderCompleted(Long orderId) {

        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));
        order.validateCompleted();
        return order;
    }

    private void incrementStockQuantity(final Long orderId) {

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        if (orderItems.isEmpty()) {
            log.warn("No order items found for orderId: {}", orderId);
            return;
        }

        // 재고 수량 복구
        redisTransactionUtil.transaction(operations -> {
            orderItems.forEach(orderItem -> {
                Long productId = orderItem.getProductId();
                int quantity = orderItem.getQuantity();
                String stockKey = "product:" + productId + ":stock";
                operations.opsForValue().increment(stockKey, quantity);
            });
        });

        log.info("Incremented stock quantity for orderId: {}", orderId);

        // 재고 히스토리 이벤트 전송
        kafkaProducer.produce("stock-history", StockHistoryMessage.builder()
            .userId(orderItems.get(0).getOrders().getUser().getId())
            .type("INCREMENT")
            .createdAt(LocalDateTime.now())
            .historyItems(orderItems.stream()
                .map(orderItem -> StockHistoryMessage.HistoryItem.builder()
                    .productId(orderItem.getProductId())
                    .quantity(orderItem.getQuantity())
                    .build())
                .toList())
            .build());
    }

    private void saveOrderCompletedStatus(Orders order) {
        ordersRepository.save(order.completeOrder());
    }

    private void saveOrderFailedCompletedStatus(Orders order) {
        ordersRepository.save(order.setOrderStatusToFailedCompleted());
        log.info("set order status to FAILED_COMPLETED for orderId: {}", order.getId());
    }
}