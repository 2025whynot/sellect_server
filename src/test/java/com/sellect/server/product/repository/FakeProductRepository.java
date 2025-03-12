package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class FakeProductRepository implements ProductRepository {

    private final List<Product> data = new ArrayList<>();
    private long nextId = 1L; // ID 자동 증가를 위한 변수

    @Override
    public List<Product> saveAll(List<Product> products) {
        products.forEach(this::save); // 개별적으로 save 호출하여 ID 할당
        return new ArrayList<>(products);
    }

    @Override
    public boolean isDuplicateProduct(Long sellerId, String name) {
        return data.stream()
            .anyMatch(product -> product.getSeller().getId().equals(sellerId) &&
                product.getName().equals(name) && product.getDeleteAt() == null);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return data.stream()
            .filter(product -> product.getId() != null && product.getId().equals(productId))
            .filter(product -> product.getDeleteAt() == null)
            .findFirst();
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            // 새로운 엔티티라면 ID 자동 할당 및 모든 필드 복사
            Product newProduct = Product.builder()
                .id(nextId++)
                .seller(product.getSeller())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .name(product.getName())
                .description(product.getDescription())
                .createdAt(product.getCreatedAt()) // 생성 시간 유지
                .updatedAt(product.getUpdatedAt()) // 업데이트 시간 유지
                .deleteAt(product.getDeleteAt())   // 삭제 시간 유지
                .build();
            data.add(newProduct);
            return newProduct;
        } else {
            // 기존 엔티티라면 업데이트
            findById(product.getId()).ifPresentOrElse(
                existingProduct -> {
                    data.remove(existingProduct);
                    data.add(product);
                },
                () -> data.add(product) // ID가 있지만 데이터에 없으면 추가
            );
            return product;
        }
    }

    @Override
    public Page<Product> findContainingName(String keyword, Pageable pageable) {
        List<Product> findProducts = data.stream()
            .filter(product -> product.getName().contains(keyword))
            .filter(product -> product.getDeleteAt() == null)
            .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), findProducts.size());
        List<Product> pagedProducts =
            (start < end) ? findProducts.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(pagedProducts, pageable, findProducts.size());
    }

    @Override
    public Page<Product> findBySellerId(Long sellerId, Pageable pageable) {
        List<Product> sellerProducts = data.stream()
            .filter(product -> product.getSeller().getId().equals(sellerId))
            .filter(product -> product.getDeleteAt() == null)
            .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sellerProducts.size());
        List<Product> pagedProducts =
            (start < end) ? sellerProducts.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(pagedProducts, pageable, sellerProducts.size());
    }

    @Override
    public List<Long> findProductIdsBySellerId(Long sellerId) {
        return data.stream()
            .filter(product -> product.getSeller().getId().equals(sellerId))
            .filter(product -> product.getDeleteAt() == null)
            .map(Product::getId)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Object> findByIdWithLock(Long id) {
        return Optional.empty();
    }

    public void clear() {
        data.clear();
        nextId = 1L; // 데이터 초기화 시 ID도 리셋
    }
}