package com.sellect.server.order.application.v1.approvepayment.v5;

import com.sellect.server.product.repository.InventoryEntity;
import com.sellect.server.product.repository.InventoryJpaRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventorySyncScheduler {

    private final InventoryJpaRepository inventoryRepository;
    private final RedisTemplate<String, Integer> redisTemplate;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final String STOCK_KEY_PREFIX = "inventory:stock:";
    private static final long SYNC_INTERVAL_MINUTES = 5;

    @PostConstruct
    public void init() {
        scheduleNextSync(0); // 서버 시작 시 즉시 실행
    }

    private void scheduleNextSync(long delayMinutes) {
        scheduler.schedule(this::syncRedisWithDatabase, delayMinutes, TimeUnit.MINUTES);
    }

    public void syncRedisWithDatabase() {
        Instant startTime = Instant.now();
        log.info("[재고 동기화] 시작: {}", startTime);

        // todo: 동기화 할때 꼭 모든 재고를 찾아볼 필요는 없다.
        // todo: 동기화 할때 굳이 JPA를 쓸 필요는 없다.
        List<InventoryEntity> inventories = inventoryRepository.findAll();
        int updatedCount = 0;


        for (InventoryEntity inventory : inventories) {
            Long productId = inventory.getProductEntity().getId();
            String key = STOCK_KEY_PREFIX + productId;

            Integer redisStock = redisTemplate.opsForValue().get(key);
            if (redisStock == null) {
                redisStock = inventory.getStock();
                redisTemplate.opsForValue().set(key, redisStock);
            }

            Integer dbStock = inventory.getStock();
            if (!dbStock.equals(redisStock)) {
                inventory.updateStock(redisStock); // Redis → DB 반영
                inventoryRepository.save(inventory);
                updatedCount++;
                log.info("[재고 동기화] productId={}, Redis={}, DB={} → DB 조정={}",
                    productId, redisStock, dbStock, redisStock);
            }
        }

        long elapsedMillis = Duration.between(startTime, Instant.now()).toMillis();
        long nextDelay = SYNC_INTERVAL_MINUTES - (elapsedMillis / 60000);
        scheduleNextSync(Math.max(nextDelay, 0));
        log.info("[재고 동기화] 완료: 실행 시간={}ms, 다음 실행까지 {}분 대기", elapsedMillis, nextDelay);
    }
}