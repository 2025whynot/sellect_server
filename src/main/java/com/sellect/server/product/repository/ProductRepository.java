package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<Product> saveAll(List<Product> products);

    // 중복 상품 검사 체크
    boolean isDuplicateProduct(Long sellerId, String name,
        LocalDateTime deleteAt);

    Page<Product> findContainingName(String keyword, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    Page<Product> findByIdIn(List<Long> ids, Pageable pageable);

    // 상품 단건 조회
    Optional<Product> findById(Long productId);

    Product save(Product product);
}
