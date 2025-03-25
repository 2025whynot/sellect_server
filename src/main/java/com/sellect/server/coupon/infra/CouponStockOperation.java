package com.sellect.server.coupon.infra;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CouponStockOperation {

    private final RedissonClient redissonClient;

    public void increaseStock(Long couponId, int couponQuantity){
        String counterKey = createKey(couponId);
        RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
        long increaseStock = counter.incrementAndGet();
        if (increaseStock > couponQuantity) {
            throw new CommonException(BError.COUPON_QUANTITY_ZERO, couponId.toString());
        }
    }
    
    public void decreaseStock(Long couponId){
        String counterKey = createKey(couponId);
        RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
        counter.decrementAndGet();
    }

    private static String createKey(Long couponId) {
        return "coupon:" + couponId + ":count";
    }

}
