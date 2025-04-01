package com.sellect.server.product.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, Long> {


    Optional<InventoryEntity> findByProductEntityIdAndDeleteAtIsNull(Long productId);

    @Query("SELECT I FROM InventoryEntity I WHERE I.productEntity.id IN :productIds AND I.deleteAt IS NULL ORDER BY I.productEntity.id")
    List<InventoryEntity> findByProductEntityIdInOrderByProductId(List<Long> productIds);

    // 비관적 락 - 읽기 가능, 수정 불가능
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT I FROM InventoryEntity I WHERE I.productEntity.id = :productId")
    Optional<InventoryEntity> findWithLockByProductEntityId(Long productId);

    // 비관적 락 - 수정 가능 (조회 가능) select 조회 가능 [기본 isolation_level 기준]
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT I FROM InventoryEntity I WHERE I.productEntity.id = :productId")
    Optional<InventoryEntity> findWithWriteLockByProductEntityId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT I FROM InventoryEntity I WHERE I.productEntity.id IN :productIds")
    List<InventoryEntity> findWithWriteLockByProductEntityIds(List<Long> productIds);
}
