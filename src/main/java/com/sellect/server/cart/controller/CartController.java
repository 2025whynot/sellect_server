package com.sellect.server.cart.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.cart.application.CartService;
import com.sellect.server.cart.controller.request.CartItemAddRequest;
import com.sellect.server.cart.controller.request.CartItemQuantityChangeRequest;
import com.sellect.server.cart.controller.response.CartItemRetrieveResponse;
import com.sellect.server.cart.domain.CartItem;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CartController {

    private final CartService cartService;

    @PostMapping("/cart")
    public ApiResponse<Void> addCartItem(
        @AuthUser User user,
        @RequestBody CartItemAddRequest request) {

        cartService.addCartItem(user, request);
        return ApiResponse.ok();
    }

    @PatchMapping("/cart")
    public ApiResponse<Void> changeCartItemQuantity(
        @AuthUser User user,
        @RequestBody CartItemQuantityChangeRequest request) {

        cartService.changeCartItemQuantity(user.getId(), request);
        return ApiResponse.ok();
    }

    @GetMapping("/cart")
    public ApiResponse<List<CartItemRetrieveResponse>> retrieveCartItems(@AuthUser User user) {

        List<CartItem> cartItems = cartService.retrieveCartItems(user.getId());
        return ApiResponse.ok(
            cartItems.stream()
                .map(CartItemRetrieveResponse::from)
                .toList()
        );
    }

    @DeleteMapping("/cart/{cartItemId}")
    public ApiResponse<Void> deleteCartItems(@AuthUser User user, @PathVariable Long cartItemId) {
        cartService.deleteCartItem(user.getId(), cartItemId);
        return ApiResponse.ok();
    }
}
