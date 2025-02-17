package com.sellect.server.search.controller.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record SearchResponse(
    String brandName,
    String productId,
    String imageUrl,
    String name,
    BigDecimal price
//    Integer reviewCount,
//    Long ratingAverage
) {

}
