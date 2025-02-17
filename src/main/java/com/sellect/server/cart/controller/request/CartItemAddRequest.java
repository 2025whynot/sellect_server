package com.sellect.server.cart.controller.request;

import lombok.Builder;

@Builder
public record CartItemAddRequest(
    Long productId,
    Integer quantity
) {

}
