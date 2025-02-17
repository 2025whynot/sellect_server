package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {

    List<Product> saveAll(List<Product> products);

    // 중복 상품 검사 체크
    boolean isDuplicateProduct(Long sellerId, String name);

    // 상품 단건 조회
    Optional<Product> findById(Long productId);

    Product save(Product product);

    Page<Product> findContainingName(String keyword, Pageable pageable);

}
