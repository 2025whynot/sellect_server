package com.sellect.server.product.controller.request;

import com.sellect.server.product.domain.ProductSearchCondition;
import com.sellect.server.product.domain.ProductSortType;
import lombok.Builder;

// todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
@Builder
public record ProductReadAllRequest(
    String keyword,
    Long categoryId,
    Long brandId,
    Integer minPrice,
    Integer maxPrice,
    ProductSortType sortType // 정렬 옵션
) {

    // todo : from 대신에 다른 메서드명 고려
    public ProductSearchCondition from() {
        return ProductSearchCondition.builder()
            .keyword(keyword)
            .categoryId(categoryId)
            .brandId(brandId)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .build();
    }
}
