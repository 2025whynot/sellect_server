package com.sellect.server.product.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByProductEntityId(Long productId);

    // 비관적 락 - 읽기 가능, 수정 불가능
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT I FROM InventoryEntity I WHERE I.productEntity.id = :productId")
    Optional<InventoryEntity> findWithLockByProductEntityId(Long productId);

    // 비관적 락 - 수정 가능 (조회 가능) select 조회 가능 [기본 isolation_level 기준]
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT I FROM InventoryEntity I WHERE I.productEntity.id = :productId")
    Optional<InventoryEntity> findWithWriteLockByProductEntityId(Long productId);
}
