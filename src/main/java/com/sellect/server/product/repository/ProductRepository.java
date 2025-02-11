package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository {

    List<Product> saveAll(List<Product> products);

    // 중복 상품 검사 체크
    Boolean isDuplicateProduct(Long sellerId, String name,
        LocalDateTime deleteAt);

}
