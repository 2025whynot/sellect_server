package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Inventory;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;

    @Override
    public Inventory save(Inventory inventory) {
        InventoryEntity inventoryEntity = inventoryJpaRepository.save(
            InventoryEntity.from(inventory));
        return inventoryEntity.toModel();
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return inventoryJpaRepository.findByProductEntityIdAndDeleteAtIsNull(productId)
            .map(InventoryEntity::toModel);
    }

    @Override
    public List<Inventory> findByProductIds(List<Long> productIds) {
        return inventoryJpaRepository.findByProductEntityIdInAndDeleteAtIsNull(productIds)
            .stream()
            .map(InventoryEntity::toModel)
            .toList();
    }

    @Override
    public Optional<Inventory> findById(Long id) {
        return inventoryJpaRepository.findById(id)
            .map(InventoryEntity::toModel);
    }

    // BEFORE[1] - 기존 비관적 락 (읽기 락)
    @Override
    public Optional<Inventory> findWithLockByProductId(Long productId) {
        return inventoryJpaRepository.findWithLockByProductEntityId(productId)
            .map(InventoryEntity::toModel);
    }

    // AFTER[1] - 새로 만든 비관적 락 (쓰기 락)
    @Override
    public Optional<Inventory> findWithWriteLockByProductId(Long productId) {
        return inventoryJpaRepository.findWithWriteLockByProductEntityId(productId)
            .map(InventoryEntity::toModel);
    }

    // AFTER[2] - 새로 만든 saveAll() vs application forEach()와 비교 - 동작 방식의 차이가 있음
    @Override
    public List<Inventory> saveAll(List<Inventory> inventories) {
        List<InventoryEntity> result = inventoryJpaRepository.saveAll(
            inventories.stream()
                .map(InventoryEntity::from)
                .toList());
        return result.stream().map(InventoryEntity::toModel).collect(Collectors.toList());
    }
}
