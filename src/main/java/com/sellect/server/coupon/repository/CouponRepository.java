package com.sellect.server.coupon.repository;

import com.sellect.server.coupon.domain.Coupon;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long couponId);

//    Page<Coupon> findAllActiveCouponList(PageRequest request);
    Page<Coupon> findAllActiveCouponList(Pageable pageable);

    Optional<Coupon> findByIdWithPessimisticLock(Long couponId);
}
