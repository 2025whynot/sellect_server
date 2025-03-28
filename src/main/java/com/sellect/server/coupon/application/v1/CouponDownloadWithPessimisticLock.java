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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class CouponDownloadWithPessimisticLock extends CouponService {
    private final CouponRepository couponRepository;
    private final UserReceivedCouponRepository userReceivedCouponRepository;
    private final ProductRepository productRepository;

    public CouponDownloadWithPessimisticLock(CouponRepository couponRepository,
        UserReceivedCouponRepository userReceivedCouponRepository,
        ProductRepository productRepository) {
        super(couponRepository, userReceivedCouponRepository, productRepository);
        this.couponRepository = couponRepository;
        this.userReceivedCouponRepository = userReceivedCouponRepository;
        this.productRepository = productRepository;
    }


    @Override
    @Transactional
    public void downloadCoupon(User user, Long couponId) {
        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, String.valueOf(couponId)));
        coupon.isUsable();
        if (userReceivedCouponRepository.existsByUserAndCoupon(user, coupon)) {
            throw new CommonException(BError.COUPON_ALREADY_RECEIVED, couponId.toString());
        }
        Coupon decreasedCoupon = coupon.decreaseQuantity();
        UserReceivedCoupon userReceivedCoupon = UserReceivedCoupon.create(user,
            decreasedCoupon);
        userReceivedCouponRepository.save(userReceivedCoupon);
        couponRepository.save(decreasedCoupon);
    }
}
