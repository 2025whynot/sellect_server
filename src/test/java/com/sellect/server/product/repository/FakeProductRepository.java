package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public Page<Product> findContainingName(String keyword, Pageable pageable) {
        List<Product> findProducts = data.stream()
                .filter(product -> product.getName().contains(keyword))
                .collect(Collectors.toList());
        return new PageImpl<>(findProducts, pageable, findProducts.size());
    }

    @Override
    public Page<Product> findByCategoryId(Long categoryId, Pageable pageable) {
        List<Product> findProducts = data.stream()
                .filter(product -> product.getCategoryId().equals(categoryId))
                .collect(Collectors.toList());
        return new PageImpl<>(findProducts, pageable, findProducts.size());
    }

    @Override
    public Page<Product> findByBrandId(Long brandId, Pageable pageable) {
        List<Product> findProducts = data.stream()
                .filter(product -> product.getBrandId().equals(brandId))
                .collect(Collectors.toList());
        return new PageImpl<>(findProducts, pageable, findProducts.size());
    }

    @Override
    public Page<Product> findByIdIn(List<Long> ids, Pageable pageable) {
        List<Product> findProducts = data.stream()
                .filter(product -> ids.contains(product.getId()))
                .collect(Collectors.toList());
        return new PageImpl<>(findProducts, pageable, findProducts.size());
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