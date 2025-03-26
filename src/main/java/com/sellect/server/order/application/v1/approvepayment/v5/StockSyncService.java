package com.sellect.server.order.application.v1.approvepayment.v5;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockSyncService {

    private final RedisTemplate<String, Integer> redisTemplate;
    private final InventoryRepository inventoryRepository;
    private static final String STOCK_KEY_PREFIX = "inventory:stock:";

    public void preloadStocksIfNeeded(List<OrderItem> items) {
        boolean needsPreload = false;

        for (OrderItem item : items) {
            String key = STOCK_KEY_PREFIX + item.getProductId();
            Integer stock = redisTemplate.opsForValue().get(key);
            if (stock == null) {
                needsPreload = true;
                break;
            }
        }

        if (needsPreload) {
            for (OrderItem item : items) {
                String key = STOCK_KEY_PREFIX + item.getProductId();
                if (redisTemplate.opsForValue().get(key) == null) { // 중복 체크
                    Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new CommonException(BError.NOT_VALID, "Product " + item.getProductId()));
                    redisTemplate.opsForValue().set(key, inventory.getStock());
                    log.info("Preloaded stock from MySQL: key={}, stock={}", key, inventory.getStock());
                }
            }
        }
    }
}
