package com.sellect.server.order.controller.response;

import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.product.domain.Product;
import java.math.BigDecimal;

public record OrderItemGetResponse(
    Long productId,
    String brandName,
    String productName,
    BigDecimal productPrice,
    int quantity,
    String imageUrl

) {

    public static OrderItemGetResponse from(OrderItem orderItem, Product product,
        String imageUrl) {
        return new OrderItemGetResponse(
            orderItem.getProductId(),
            product.getBrand().getName(), // todo: N+1 발생 원인 부분 (임시로 일단 구현에 집중)
            product.getName(),
            product.getPrice(),
            orderItem.getQuantity(),
            imageUrl
        );
    }
}
