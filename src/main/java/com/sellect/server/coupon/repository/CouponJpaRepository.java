package com.sellect.server.coupon.repository;

import com.sellect.server.coupon.repository.entity.CouponEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {

    Page<CouponEntity> findByCreatedAtNullAndQuantityGreaterThanAndExpirationDateAfter(
        Integer quantity, LocalDate expirationDate, Pageable pageable);

    Page<CouponEntity> findByDeleteAtNullAndQuantityGreaterThanAndExpirationDateAfter(
        Integer quantity, LocalDate expirationDate, Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponEntity c WHERE c.id = :id")
    Optional<CouponEntity> findByIdWithPessimisticLock(@Param("id")Long id);
}
