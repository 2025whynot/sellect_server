package com.sellect.server.cart.controller.response;

import com.sellect.server.cart.domain.CartItem;
import com.sellect.server.product.domain.Product;
import lombok.Builder;

@Builder
public record CartItemRetrieveResponse(
    Long cartItemId,
    Product product,
    Integer quantity
) {

    public static CartItemRetrieveResponse from(CartItem cartItem) {
        return CartItemRetrieveResponse.builder()
            .cartItemId(cartItem.getId())
            .product(cartItem.getProduct())
            .quantity(cartItem.getQuantity())
            .build();
    }

}
