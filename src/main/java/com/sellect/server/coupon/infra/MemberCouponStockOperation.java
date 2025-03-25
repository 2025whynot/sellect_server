package com.sellect.server.coupon.infra;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class MemberCouponStockOperation {

    private final RedissonClient redissonClient;

    public void add(Long couponId, User user) {
        String userCouponKey = createKey(couponId);
        RSet<String> userSet = redissonClient.getSet(userCouponKey);
        String userIdStr = user.getId().toString();
        if (!userSet.add(userIdStr)) {
            throw new CommonException(BError.ALREADY_RECEIVED, couponId.toString());
        }
    }

    public void remove(Long couponId, Long userId) {
        String userCouponKey = createKey(couponId);
        RSet<String> userSet = redissonClient.getSet(userCouponKey);
        userSet.remove(userId.toString());
    }

    public int totalUsedCount(Long couponId, User user){
        String userCouponKey = createKey(couponId);
        RSet<String> userSet = redissonClient.getSet(userCouponKey);
        return userSet.size();
    }

    private String createKey(Long couponId) {
        return "coupon:" + couponId + ":users";
    }
}
