package com.sellect.server.coupon.repository;

import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.coupon.repository.entity.CouponEntity;
import com.sellect.server.coupon.repository.entity.UserReceivedCouponEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserReceivedCouponJpaRepository extends
    JpaRepository<UserReceivedCouponEntity, Long> {

    List<UserReceivedCouponEntity> findAllByUser(UserEntity user, Pageable pageable);

    List<UserReceivedCouponEntity> findByUserAndIsUsed(UserEntity from, PageRequest pageRequest,
        Boolean isUsed);

    List<UserReceivedCouponEntity> findAllByUserAndIsUsed(UserEntity user, boolean isUsed);

    Optional<UserReceivedCouponEntity> findByUserAndCoupon(UserEntity from, CouponEntity coupon);

    Boolean existsByUserAndCoupon(UserEntity user, CouponEntity coupon);

    // 쿠폰 중복 사용을 막기 위한 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM UserReceivedCouponEntity c WHERE c.id = :id")
    Optional<UserReceivedCouponEntity> findWithWriteLockById(@Param("id") Long id);
}
