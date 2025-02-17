package com.sellect.server.cart.domain;

import com.sellect.server.auth.domain.User;
import com.sellect.server.product.domain.Product;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartItem {

    private Long id;
    private User user;
    private Product product;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deleteAt;

    public static CartItem register(User user, Product product, Integer quantity) {
        return CartItem.builder()
            .user(user)
            .product(product)
            .quantity(quantity)
            .build();
    }

    public CartItem changeQuantity(Integer quantity) {
        return CartItem.builder()
            .id(this.id)
            .user(this.user)
            .product(this.product)
            .quantity(quantity)
            .build();
    }

    public CartItem remove() {
        return CartItem.builder()
            .id(this.id)
            .user(this.user)
            .product(this.product)
            .quantity(null)
            .deleteAt(LocalDateTime.now())
            .build();
    }

}