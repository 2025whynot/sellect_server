package com.sellect.server.order.application.v1.approvepayment.v5;

import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final InventoryRepository inventoryRepository;
    private final RedisTemplate<String, Integer> redisTemplate;
    private static final String STOCK_KEY_PREFIX = "inventory:stock:";

    public StockDeductionResult tryDeductStocks(List<OrderItem> items) {
        Map<String, Integer> deducted = new HashMap<>();
        boolean success = true;

        for (OrderItem item : items) {
            String key = STOCK_KEY_PREFIX + item.getProductId();
            Integer stock = redisTemplate.opsForValue().get(key);
            if (stock == null || stock < item.getQuantity()) {
                success = false;
                break;
            }
            redisTemplate.opsForValue().decrement(key, item.getQuantity());
            deducted.put(key, item.getQuantity());
        }

        return new StockDeductionResult(
            success,
            deducted,
            () -> rollbackStocks(deducted)
        );
    }

    private void rollbackStocks(Map<String, Integer> deducted) {
        for (Map.Entry<String, Integer> entry : deducted.entrySet()) {
            String key = entry.getKey();
            Integer qty = entry.getValue();
            int retries = 3;
            while (retries > 0) {
                try {
                    redisTemplate.opsForValue().increment(key, qty);
                    log.info("Rolled back stock: key={}, qty={}", key, qty);
                    return;
                } catch (Exception e) {
                    retries--;
                    if (retries == 0) {
                        log.error("Redis 재고 복구 실패: key={}, qty={}, error={}",
                            key, qty, e.getMessage(), e);
                        // TODO: 알림 추가
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
