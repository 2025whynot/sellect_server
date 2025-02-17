package com.sellect.server.product.domain;

import com.sellect.server.product.controller.request.ImageContextUpdateRequest;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductImage {

    private final Long id;
    private final Product product;
    private final String imageUrl;
    private final boolean representative;
    private final String uuid;
    private final String prev;
    private final String next;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    public static ProductImage registerWhenUpdate(Product product, String imageUrl, ImageContextUpdateRequest request) {
        return ProductImage.builder()
            .product(product)
            .imageUrl(imageUrl)
            .representative(request.isRepresentative())
            .uuid(request.target())
            .prev(request.prev())
            .next(request.next())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deleteAt(null)
            .build();
    }

    // TODO: request dto 의존적임 -> 수정 필요
    public ProductImage update(ImageContextUpdateRequest request) {
        return ProductImage.builder()
            .id(this.id)
            .product(this.product)
            .imageUrl(this.imageUrl)
            .representative(request.isRepresentative())
            .uuid(request.target())
            .prev(request.prev())
            .next(request.next())
            .createdAt(this.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    public ProductImage updateImageUrl(String imageUrl) {
        return ProductImage.builder()
            .id(this.id)
            .product(this.product)
            .imageUrl(imageUrl)
            .representative(this.representative)
            .uuid(this.uuid)
            .prev(this.prev)
            .next(this.next)
            .createdAt(this.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    public ProductImage remove() {
        return ProductImage.builder()
            .id(this.id)
            .product(this.product)
            .imageUrl(this.imageUrl)
            .representative(this.representative)
            .uuid(this.uuid)
            .prev(this.prev)
            .next(this.next)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(LocalDateTime.now()) // 삭제 시간 업데이트
            .build();
    }
}
