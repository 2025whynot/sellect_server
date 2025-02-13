package com.sellect.server.product.controller.response;

import com.sellect.server.product.domain.Product;
import java.math.BigDecimal;

public record ProductModifyResponse(
    Long productId,
    String name,
    BigDecimal price,
    Integer stock

) {

    public static ProductModifyResponse from(Product product) {
        return new ProductModifyResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStock()
        );
    }
}
