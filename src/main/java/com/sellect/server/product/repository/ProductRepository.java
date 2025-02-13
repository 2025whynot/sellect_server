package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductSearchCondition;
import com.sellect.server.product.domain.ProductSortType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {

    // 상품 조회 (동적 쿼리)
    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    List<Product> search(ProductSearchCondition condition, int page, int size, ProductSortType sortType);

    List<Product> saveAll(List<Product> products);

    // 중복 상품 검사 체크
    boolean isDuplicateProduct(Long sellerId, String name);

    // 상품 단건 조회
    Optional<Product> findById(Long productId);

    Product save(Product product);

    Page<Product> findContainingName(String keyword, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    Page<Product> findByIdIn(List<Long> ids, Pageable pageable);

}
