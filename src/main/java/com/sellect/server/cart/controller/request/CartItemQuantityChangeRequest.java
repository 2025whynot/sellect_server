package com.sellect.server.cart.controller.request;

import lombok.Builder;

@Builder
public record CartItemQuantityChangeRequest(
    Long cartItemId,
    Integer quantity
) {

}
