package com.sellect.server.coupon.repository;

import com.sellect.server.coupon.domain.Coupon;
import com.sellect.server.coupon.repository.entity.CouponEntity;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity save = couponJpaRepository.save(CouponEntity.from(coupon));
        return save.toModel();
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        Optional<CouponEntity> couponEntity = couponJpaRepository.findById(couponId);
        return couponEntity.map(CouponEntity::toModel);
    }

    @Override
    public Page<Coupon> findAllActiveCouponList(Pageable pageable) {
        Page<CouponEntity> activeCouponList = couponJpaRepository.findByDeleteAtNullAndQuantityGreaterThanAndExpirationDateAfter(
            0,
            LocalDate.now(), pageable);
        return activeCouponList.map(CouponEntity::toModel);
    }

    @Override
    public Optional<Coupon> findByIdWithPessimisticLock(Long couponId) {
        Optional<CouponEntity> couponEntity = couponJpaRepository.findByIdWithPessimisticLock(
            couponId);
        return couponEntity.map(CouponEntity::toModel);
    }
}
