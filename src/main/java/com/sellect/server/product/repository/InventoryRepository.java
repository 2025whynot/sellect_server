package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Inventory;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {

    Inventory save(Inventory inventory);

    Optional<Inventory> findByProductId(Long productId);

    List<Inventory> findByProductIdsOrderByProductId(List<Long> productIds);

    Optional<Inventory> findById(Long id);

    // BEFORE - READ_LOCK
    Optional<Inventory> findWithLockByProductId(Long productId);

    // AFTER - WRITE_LOCK
    Optional<Inventory> findWithWriteLockByProductId(Long productId);

    //
    List<Inventory> saveAll(List<Inventory> inventories);
}
