package com.sellect.server.coupon.application.v1;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.coupon.application.CouponService;
import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.domain.UserReceivedCoupon;
import com.sellect.server.coupon.repository.CouponRepository;
import com.sellect.server.coupon.repository.UserReceivedCouponRepository;
import com.sellect.server.product.repository.ProductRepository;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;


@Service
public class CouponDownloadWithDistributedLock extends CouponService {
    private final CouponRepository couponRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;

    public CouponDownloadWithDistributedLock(CouponRepository couponRepository,
        UserReceivedCouponRepository userReceivedCouponRepository,
        ProductRepository productRepository, RedissonClient redissonClient) {
        super(couponRepository, userReceivedCouponRepository, productRepository);
        this.couponRepository = couponRepository;
        this.userReceivedCouponRepository = userReceivedCouponRepository;
        this.productRepository = productRepository;
        this.redissonClient = redissonClient;
    }


    @Override
    public void downloadCoupon(User user, Long couponId) {
        String lockKey = String.format("coupon:couponLock:%d", couponId);
        RLock lock = redissonClient.getLock(lockKey);

        // 락 획득 시도: 최대 5초 대기, 락 유지 시간 2초
        try {
            boolean isLock = lock.tryLock(5, 2, TimeUnit.SECONDS);
            if (!isLock) {
                throw new CommonException(BError.LOCK_ACQUISITION_FAILED, couponId.toString());
            }
            Coupon coupon = couponRepository.findById(couponId).orElseThrow();
            coupon.isUsable();
            if (userReceivedCouponRepository.existsByUserAndCoupon(user, coupon)) {
                throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
            }
            Coupon decreasedCoupon = coupon.decreaseQuantity();
            UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user,
                decreasedCoupon);
            userReceivedCouponRepository.save(userReceivedCoupon);
            couponRepository.save(decreasedCoupon);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
