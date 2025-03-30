package com.sellect.server.coupon.application.downloadcoupon;

import com.sellect.server.auth.domain.User;
import com.sellect.server.coupon.application.v1.CouponDownloadWithDistributedLock;
import com.sellect.server.coupon.application.v1.CouponDownloadWithPessimisticLock;
import com.sellect.server.coupon.application.v1.CouponDownloadWithReentrantLock;
import com.sellect.server.coupon.application.v2.CouponDownloadWithRedisAndEvent;
import com.sellect.server.coupon.application.v3.CouponDownloadWithRedisSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DownLoadCoupon {

    private final CouponDownloadWithReentrantLock couponDownloadWithReentrantLock;
    private final CouponDownloadWithPessimisticLock couponDownloadWithPessimisticLock;
    private final CouponDownloadWithDistributedLock couponDownloadWithDistributedLock;
    private final CouponDownloadWithRedisAndEvent couponDownloadWithRedisAndEvent;
    private final CouponDownloadWithRedisSet couponDownloadWithRedisSet;

    public void couponDownloadWithReentrantLock(User user, Long couponId) {
        couponDownloadWithReentrantLock.downloadCoupon(user, couponId);
    }

    public void setCouponDownloadWithPessimisticLock(User user, Long couponId) {
        couponDownloadWithPessimisticLock.downloadCoupon(user, couponId);
    }

    public void setCouponDownloadWithDistributedLock(User user, Long couponId) {
        couponDownloadWithDistributedLock.downloadCoupon(user, couponId);
    }

    public void setCouponDownloadWithRedisAndEvent(User user, Long couponId) {
        couponDownloadWithRedisAndEvent.downloadCoupon(user, couponId);
    }

    public void downloadWithRedisSet(User user, Long couponId) {
        couponDownloadWithRedisSet.downloadCoupon(user, couponId);
    }
}
