package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Inventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeInventoryRepository implements InventoryRepository {

    private final List<Inventory> data = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public Inventory save(Inventory inventory) {
        if (inventory.getId() == null) {
            Inventory newInventory = Inventory.builder()
                .id(nextId++)
                .product(inventory.getProduct())
                .stock(inventory.getStock())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .deleteAt(inventory.getDeleteAt())
                .build();
            data.add(newInventory);
            return newInventory;
        } else {
            findById(inventory.getId()).ifPresentOrElse(
                existingInventory -> {
                    data.remove(existingInventory);
                    data.add(inventory);
                },
                () -> data.add(inventory)
            );
            return inventory;
        }
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return data.stream()
            .filter(inventory -> inventory.getId() != null &&
                inventory.getProduct().getId().equals(productId))
            .filter(inventory -> inventory.getDeleteAt() == null)
            .findFirst();
    }

    @Override
    public List<Inventory> findByProductIds(List<Long> productIds) {
        return data.stream()
            .filter(inventory -> inventory.getId() != null &&
                productIds.contains(inventory.getProduct().getId()))
            .filter(inventory -> inventory.getDeleteAt() == null)
            .toList();
    }

    @Override
    public Optional<Inventory> findById(Long id) {
        return data.stream()
            .filter(inventory -> inventory.getId() != null &&
                inventory.getId().equals(id))
            .filter(inventory -> inventory.getDeleteAt() == null)
            .findFirst();
    }


    @Override
    public Optional<Inventory> findWithLockByProductId(Long productId) {
        return data.stream()
            .filter(inventory -> inventory.getId() != null &&
                inventory.getProduct().getId().equals(productId))
            .filter(inventory -> inventory.getDeleteAt() == null)
            .findFirst();
    }

    public void clear() {
        data.clear();
        nextId = 1L;
    }


    // todo : 테스트시 작성 필요
    @Override
    public Optional<Inventory> findWithWriteLockByProductId(Long productId) {
        return Optional.empty();
    }

    // todo : 테스트시 작성 필요
    @Override
    public List<Inventory> saveAll(List<Inventory> inventories) {
        return List.of();
    }

}
