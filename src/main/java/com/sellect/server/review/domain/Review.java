package com.sellect.server.review.domain;

import com.sellect.server.auth.domain.User;
import com.sellect.server.product.domain.Product;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
public class Review {
    private final Long id;
    private final User user;
    private final Product product;
    private final float rating;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    public static Review register(User user, Product product, float rating, String description) {
        return Review.builder()
            .user(user)
            .product(product)
            .rating(rating)
            .description(description)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deleteAt(null)
            .build();
    }
}
