package com.sellect.server.product.domain;

import com.sellect.server.product.controller.request.ImageContextUpdateRequest;
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
    private final String uuid;
    private final String prev;
    private final String next;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    public ProductImage update(ImageContextUpdateRequest request) {
        if (request.isPrev()) {
            return ProductImage.builder()
                .id(this.id)
                .product(this.product)
                .imageUrl(this.imageUrl)
                .representative(this.representative)
                .uuid(this.uuid)
                .prev(request.targetUUID())
                .next(this.next)
                .createdAt(this.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .deleteAt(this.deleteAt)
                .build();
        } else {
            return ProductImage.builder()
                .id(this.id)
                .product(this.product)
                .imageUrl(this.imageUrl)
                .representative(this.representative)
                .uuid(this.uuid)
                .prev(this.prev)
                .next(request.targetUUID())
                .createdAt(this.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .deleteAt(this.deleteAt)
                .build();
        }
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
