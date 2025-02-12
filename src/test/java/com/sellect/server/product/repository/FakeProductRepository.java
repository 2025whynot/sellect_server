package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public Optional<Product> findById(Long productId) {
        return data.stream()
            .filter(product -> product.getId().equals(productId))
            .filter(product -> product.getDeleteAt() == null)
            .findFirst();
    }

    @Override
    public Product save(Product product) {
        findById(product.getId()).ifPresentOrElse(
            existingProduct -> {
                // 기존 데이터 업데이트 (삭제 후 재등록)
                data.remove(existingProduct);
                data.add(product);
            },
            () -> data.add(product) // 새로운 데이터 추가
        );

        return product;
    }


    public List<Product> findAll() {
        return new ArrayList<>(data);
    }

    public void clear() {
        data.clear();
    }

}