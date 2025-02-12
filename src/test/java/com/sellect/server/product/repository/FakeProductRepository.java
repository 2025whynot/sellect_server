package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FakeProductRepository implements ProductRepository {

    private final List<Product> data = new ArrayList<>();

    @Override
    public List<Product> saveAll(List<Product> products) {
        data.addAll(products);
        return new ArrayList<>(data);
    }

    @Override
    public boolean isDuplicateProduct(Long sellerId, String name, LocalDateTime deleteAt) {
        return data.stream()
                .anyMatch(product -> product.getSellerId().equals(sellerId) &&
                        product.getName().equals(name) &&
                        (deleteAt == null || product.getDeleteAt() == null));
    }

    @Override
    public List<Product> findContainingName(String keyword) {
        return List.of();
    }

    @Override
    public List<Product> findByCategoryId(Long categoryId) {
        return List.of();
    }

    @Override
    public List<Product> findByBrandId(Long brandId) {
        return List.of();
    }

    @Override
    public List<Product> findByIdIn(List<Long> ids) {
        return List.of();
    }

    public List<Product> save(Product product) {
        data.add(product);
        return new ArrayList<>(data);
    }

    public List<Product> findAll() {
        return new ArrayList<>(data);
    }

    public void clear() {
        data.clear();
    }

}