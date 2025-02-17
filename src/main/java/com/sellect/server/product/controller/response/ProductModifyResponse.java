package com.sellect.server.product.controller.response;

import com.sellect.server.product.domain.Product;
import java.math.BigDecimal;

public record ProductModifyResponse(
    String name,
    BigDecimal price,
    Integer stock

) {

    public static ProductModifyResponse from(Product product) {
        return new ProductModifyResponse(
            product.getName(),
            product.getPrice(),
            product.getStock()
        );
    }
}
