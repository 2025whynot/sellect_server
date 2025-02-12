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

    // todo : 상품 수정 테스트 시 구현
    @Override
    public Optional<Product> findById(Long productId) {
        return Optional.empty();
    }

    // todo: 상품 수정 테스트 시 구현
    @Override
    public Product save(Product product) {
        return null;
    }


    public List<Product> findAll() {
        return new ArrayList<>(data);
    }

    public void clear() {
        data.clear();
    }

}