package com.sellect.server.coupon.application.v2;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.application.CouponService;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.event.CouponDownloadEvent;
import com.sellect.server.coupon.repository.CouponRepository;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponDownloadWithRedisAndEvent  {

    private final CouponRepository couponRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void downloadCoupon(User user, Long couponId) {
        String counterKey = "coupon:" + couponId + ":count";
        String userCouponKey = "coupon:" + couponId + ":users";
        RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
        RSet<String> userSet = redissonClient.getSet(userCouponKey);

        if (!counter.isExists()) {
            Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CommonException(BError.NOT_EXIST, couponId.toString()));
            counter.set(coupon.getQuantity());
        }

        // Redis에서 수량 감소
        long remaining = counter.get();
        if (remaining <= 0) {
            throw new CommonException(BError.COUPON_QUANTITY_ZERO, couponId.toString());
        }
        counter.decrementAndGet();
        try {
            // 수량 체크
            Coupon coupon = couponRepository.findById(couponId).orElseThrow(() ->
                new CommonException(BError.NOT_EXIST, couponId.toString()));

            // 중복 체크
            String userIdStr = user.getId().toString();
            if (!userSet.add(userIdStr)) {
                counter.incrementAndGet(); // 롤백
                throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
            }

            // 사용자 쿠폰 저장
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user, coupon);
            userReceivedCouponRepository.save(userReceivedCoupon);
            // 이벤트 발행
            eventPublisher.publishEvent(new CouponDownloadEvent(couponId));
        } catch (Exception e) {
            // 트랜잭션 롤백 시 Redis도 롤백
            counter.incrementAndGet();
            userSet.remove(user.getId().toString());
            throw e;
        }
    }
}
