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
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
@RequiredArgsConstructor
public class CouponDownloadWithReentrantLock {

    ReentrantLock lock = new ReentrantLock();
    private final CouponRepository couponRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductRepository productRepository;
    private final PlatformTransactionManager transactionManager;


    public void downloadCoupon(User user, Long couponId) {
        lock.lock();
        try {
            TransactionStatus status = transactionManager.getTransaction(
                new DefaultTransactionDefinition());
            try {
                Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(
                        () -> new CommonException(BError.NOT_EXIST, String.valueOf(couponId)));

                coupon.validateDownload();
                if (userReceivedCouponRepository.existsByUserAndCoupon(user, coupon)) {
                    throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
                }
                Coupon decreasedCoupon = coupon.decreaseQuantity();
                UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user,
                    decreasedCoupon);
                userReceivedCouponRepository.save(userReceivedCoupon);
                couponRepository.save(decreasedCoupon);

                transactionManager.commit(status);
            } catch (Exception e) {
                transactionManager.rollback(status);
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }
}
