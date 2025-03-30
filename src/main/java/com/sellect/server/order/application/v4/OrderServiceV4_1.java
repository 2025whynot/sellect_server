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
import com.sellect.server.order.repository.OrderItemRepository;
import com.sellect.server.order.repository.OrdersRepository;
import com.sellect.server.payment.event.message.PayReadyMessage;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceV4_1 { // v4.0에서 Redis로 재고 관리하는 것만 추가

    private static final String REDIS_KEY_PREFIX = "pay-ready:redirect:";
    private static final String RETRY_KEY_PREFIX = "pay-ready:retry-count:";
    private static final int MAX_RETRY_COUNT = 5;

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
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

        kafkaProducer.produce("pay-ready", PayReadyMessage.builder()
            .orderId(order.getId())
            .userId(order.getUser().getId())
            .totalPrice(order.getTotalPrice().intValue())
            .build());
    }

    @Transactional
    public void completeOrder(final Long orderId) {
        Orders order = findOrderNotCompleted(orderId);

        try {
//            processInventories(order); // v4.0
            incrementStockUsage(orderId); // v4.1
        } catch (Exception e) {
            log.error("Failed to increment stock usage for orderId: {}", orderId, e);
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
    public void rollbackOrder(Long orderId) {
        log.info("Processing order-complete-failed for orderId: {}", orderId);

        // 재고 사용량 롤백
        decrementStockUsage(orderId);

        // 주문 상태 롤백
        Orders order = findOrderCompleted(orderId);
        rollbackOrdersStatus(order);
    }

    public String getPaymentUrl(final Long userId, final Long orderId) {

        // 주문 조회
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));

        // 유저의 주문인지 확인
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "user")); // TODO: test용 삭제
        order.validateOwner(user); // TODO: 파라미터를 User 타입으로 받아야 할 필요있는지 체크

        return getRedirectUrlFromRedis(userId, orderId);
    }

    //== private methods ==//

    private Orders findOrderNotCompleted(Long orderId) {

        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));
        order.validateNotCompleted();
        return order;
    }

    private String getRedirectUrlFromRedis(Long userId, Long orderId) {
        String redirectUrlKey = REDIS_KEY_PREFIX + orderId;
        String retryCountKey = RETRY_KEY_PREFIX + userId + ":" + orderId;

        // 재시도 횟수 체크 및 증가 (원자적 연산)
        Long retryCount = redisTemplate.opsForValue().increment(retryCountKey, 1L);
        if (retryCount == null) retryCount = 1L; // 초기 값 처리

        if (retryCount > MAX_RETRY_COUNT) {
            log.warn("Max retry count exceeded for userId: {}, orderId: {}", userId, orderId);
            throw new CommonException(BError.FAIL_FOR_REASON, "get redirect URL from Redis",
                "Max retry count exceeded");
        }

        // TTL 설정
        if (retryCount == 1) {
            redisTemplate.expire(retryCountKey, 10, TimeUnit.MINUTES);
        }

        // Redirect URL 조회
        String redirectUrl = redisTemplate.opsForValue().get(redirectUrlKey);
        if (redirectUrl == null) {
            log.debug("No redirect URL found in Redis for key: {}", redirectUrlKey);
        } else {
            log.debug("Retrieved redirect URL from Redis: key={}, value={}", redirectUrlKey, redirectUrl);
        }
        return redirectUrl;
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
    private void incrementStockUsage(final Long orderId) {

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
    }

    private List<OrderItem> findOrderItemsSortedByProductId(Long orderId) {

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        return orderItems.stream()
            .sorted(Comparator.comparingLong(OrderItem::getProductId))
            .toList();
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
            requestQuantities.put(stockUsageKey, requestQuantity);
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

    private Orders findOrderCompleted(Long orderId) {

        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "order"));
        order.validateCompleted();
        return order;
    }

    private void decrementStockUsage(final Long orderId) {

        List<OrderItem> orderItems = orderItemRepository.findAllByOrdersId(orderId);
        if (orderItems.isEmpty()) {
            log.warn("No order items found for orderId: {}", orderId);
            return;
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
    }

    private void saveOrderCompletedStatus(Orders order) {
        ordersRepository.save(order.completeOrder());
    }

    private void rollbackOrdersStatus(Orders order) {
        ordersRepository.save(order.rollbackOrder());
        log.info("Rolled back order status to PENDING for orderId: {}", order.getId());
    }
}