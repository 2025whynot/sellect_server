package com.sellect.server.product.controller.response;

import com.sellect.server.product.domain.Product;
import java.util.List;

public record ProductRegisterResponse(
    Long productId
) {

    public static ProductRegisterResponse from(Product product) {
        return new ProductRegisterResponse(
            product.getId()
        );
    }

    public static List<ProductRegisterResponse> fromList(List<Product> products) {
        return products.stream()
            .map(ProductRegisterResponse::from)
            .toList();
    }
}
