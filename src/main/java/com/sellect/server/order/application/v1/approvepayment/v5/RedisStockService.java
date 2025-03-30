package com.sellect.server.order.application.v1.approvepayment.v5;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.redis.IntegerRedisTransactionUtil;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final InventoryRepository inventoryRepository;
    private final IntegerRedisTransactionUtil transactionUtil;
    private static final String STOCK_KEY_PREFIX = "inventory:stock:";

    public StockDeductionResult tryDeductStocks(List<OrderItem> items) {
        try {
            return transactionUtil.transaction(operations -> {
                Map<String, Integer> deducted = new HashMap<>();

                // 1. 모든 키의 재고 확인 및 초기화
                for (OrderItem item : items) {
                    String key = STOCK_KEY_PREFIX + item.getProductId();
                    Integer stock = (Integer) operations.opsForValue().get(key);
                    log.info("Checking stock in transaction: key={}, stock={}, quantity={}", key, stock, item.getQuantity());

                    // Redis에 값이 없으면 DB에서 가져오기
                    if (stock == null) {
                        Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                            .orElseThrow(() -> new CommonException(BError.NOT_VALID, "Product " + item.getProductId()));
                        stock = inventory.getStock();
                        operations.opsForValue().set(key, stock);
                        log.info("Preloaded stock in transaction: key={}, stock={}", key, stock);
                    }

                    // 재고 부족 체크
                    if (stock < item.getQuantity()) {
                        log.warn("Stock insufficient: key={}, stock={}, quantity={}", key, stock, item.getQuantity());
                        return new StockDeductionResult(false, new HashMap<>());
                    }
                }

                // 2. 모든 키의 재고 차감
                for (OrderItem item : items) {
                    String key = STOCK_KEY_PREFIX + item.getProductId();
                    operations.opsForValue().decrement(key, item.getQuantity());
                    deducted.put(key, item.getQuantity());
                    log.info("Deducted stock: key={}, quantity={}", key, item.getQuantity());
                }

                return new StockDeductionResult(true, deducted);
            });
        } catch (Exception e) {
            log.error("Redis 트랜잭션 실패: {}", e.getMessage(), e);
            return new StockDeductionResult(false, new HashMap<>());
        }
    }

    public void rollbackStocks(Map<String, Integer> deducted) {
        if (deducted.isEmpty()) {
            return;
        }
        int retries = 3;
        while (retries > 0) {
            try {
                transactionUtil.transaction(operations -> {
                    for (Map.Entry<String, Integer> entry : deducted.entrySet()) {
                        operations.opsForValue().increment(entry.getKey(), entry.getValue());
                    }
                    return null;
                });
                log.info("Redis 재고 복구 완료: {}", deducted);
                return;
            } catch (Exception e) {
                retries--;
                if (retries == 0) {
                    log.error("Redis 재고 복구 실패: deducted={}, error={}", deducted, e.getMessage(), e);
                    throw new RuntimeException("Redis 재고 복구 실패", e);
                }
                try {
                    Thread.sleep(100); // 재시도 전 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}