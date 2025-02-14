package com.sellect.server.product.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
public class ProductImage {

    private final Long id;
    private final Product product;
    private final String imageUrl;
    private final boolean representative;
    private final int sequence;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;
}
