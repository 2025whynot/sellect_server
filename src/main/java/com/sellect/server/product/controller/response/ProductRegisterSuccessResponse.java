package com.sellect.server.product.controller.response;

import com.sellect.server.product.domain.Product;
import java.util.List;

public record ProductRegisterSuccessResponse(
    Long productId,
    String name
) {

    /*
    * 개별 상품을 성공 응답 객체로 변환
    * */
    public static ProductRegisterSuccessResponse from(Product product) {
        return new ProductRegisterSuccessResponse(
            product.getId(),
            product.getName()
        );
    }

    /*
     * 리스트로 변환
     * */
    public static List<ProductRegisterSuccessResponse> fromList(List<Product> products) {
        return products.stream()
            .map(ProductRegisterSuccessResponse::from)
            .toList();
    }
}
