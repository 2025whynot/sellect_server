package com.sellect.server.search.controller.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record SearchResponse(
    // todo : 브랜드, 리뷰, 이미지 엔티티 생성 후 다시 돌아올 것
    String brandName,
    String productId,
    String imageUrl,
    String name,
    BigDecimal price,
    Integer reviewCount,
    Long ratingAverage
    // todo: 상품 이미지
) {

}
