package com.sellect.server.product.repository;

import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductImage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
@SuperBuilder // BaseTimeEntity도 Builder를 사용하므로 @SuperBuilder 필요
@Entity
@Table(name = "product_image")
public class ProductImageEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // todo: 관계 체크
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity productEntity;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private boolean representative;

    private String uuid;
    private String prev;
    private String next;

    public static ProductImageEntity from(ProductImage productImage, Product product) {
        return ProductImageEntity.builder()
            .id(productImage.getId())
            .productEntity(ProductEntity.from(product))
            .imageUrl(productImage.getImageUrl())
            .representative(productImage.isRepresentative())
            .uuid(productImage.getUuid())
            .prev(productImage.getPrev())
            .next(productImage.getNext())
            .createdAt(productImage.getCreatedAt())
            .updatedAt(productImage.getUpdatedAt())
            .deleteAt(productImage.getDeleteAt())
            .build();
    }

    public ProductImage toModel() {
        return ProductImage.builder()
            .id(this.id)
            .product(this.productEntity.toModel())
            .imageUrl(this.imageUrl)
            .representative(this.representative)
            .uuid(this.uuid)
            .prev(this.prev)
            .next(this.next)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .build();
    }
}
