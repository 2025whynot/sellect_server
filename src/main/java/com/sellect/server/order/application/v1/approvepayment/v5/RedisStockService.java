package com.sellect.server.order.application.v1.approvepayment.v5;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.redis.IntegerRedisTransactionUtil;
import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.repository.InventoryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final InventoryRepository inventoryRepository;
    private final IntegerRedisTransactionUtil transactionUtil;
    private static final String STOCK_KEY_PREFIX = "inventory:stock:";

    // Lua 스크립트: 모든 productId에 대해 재고 확인 및 차감
    private static final String LUA_SCRIPT =
        "local results = {} " +
            "for i, key in ipairs(KEYS) do " +
            "    local stock = redis.call('GET', key) " +
            "    if stock == false then " +
            "        stock = ARGV[i * 2 - 1] " +
            "        redis.call('SET', key, stock) " +
            "    end " +
            "    stock = tonumber(stock) " +
            "    local quantity = tonumber(ARGV[i * 2]) " +
            "    if stock < quantity then " +
            "        return {-1} " + // 재고 부족 시 즉시 종료
            "    end " +
            "    redis.call('DECRBY', key, quantity) " +
            "    results[i] = stock - quantity " +
            "end " +
            "return results";

    public StockDeductionResult tryDeductStocks(List<OrderItem> items) {
        try {
            Map<String, Integer> deducted = new HashMap<>();
            DefaultRedisScript<List> script = new DefaultRedisScript<>(LUA_SCRIPT, List.class);

            // Lua 스크립트에 전달할 KEYS와 ARGV 준비
            List<String> keys = new ArrayList<>();
            List<Object> args = new ArrayList<>();

            // 초기값 준비
            for (OrderItem item : items) {
                String key = STOCK_KEY_PREFIX + item.getProductId();
                keys.add(key);

                Integer stock = transactionUtil.getIntegerRedisTemplate().opsForValue().get(key);
                if (stock == null) {
                    Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new CommonException(BError.NOT_VALID, "Product " + item.getProductId()));
                    stock = inventory.getStock();
                    log.info("Preloaded stock from DB: key={}, stock={}", key, stock);
                }
                args.add(stock);         // ARGV[i * 2 - 1]: 초기 stock
                args.add(item.getQuantity()); // ARGV[i * 2]: quantity
            }

            // 단일 Lua 스크립트 실행
            List<Long> results = transactionUtil.getIntegerRedisTemplate().execute(script, keys, args.toArray());

            // 결과 처리
            if (results != null && results.contains(-1L)) {
                log.warn("Stock insufficient for one or more items");
                rollbackStocks(deducted); // 실패 시 롤백
                return new StockDeductionResult(false, new HashMap<>());
            }

            // 성공 시 deducted 채우기
            for (int i = 0; i < items.size(); i++) {
                String key = keys.get(i);
                Integer quantity = items.get(i).getQuantity();
                deducted.put(key, quantity);
//                log.info("Deducted stock: key={}, quantity={}", key, quantity);
            }

            return new StockDeductionResult(true, deducted);
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