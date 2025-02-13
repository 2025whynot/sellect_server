package com.sellect.server.product.repository;

import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "product")
public class ProductEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private Long brandId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer stock;


    public static ProductEntity from(Product product) {
        return ProductEntity.builder()
            .id(product.getId())
            .sellerId(product.getSellerId())
            .categoryId(product.getCategoryId())
            .brandId(product.getBrandId())
            .price(product.getPrice())
            .name(product.getName())
            .stock(product.getStock())
            .createdAt(product.getCreatedAt()) // BaseTimeEntity 필드 포함
            .updatedAt(product.getUpdatedAt()) // BaseTimeEntity 필드 포함
            .deleteAt(product.getDeleteAt()) // BaseTimeEntity 필드 포함
            .build();
    }

    // ProductEntity -> Product 도메인 객체 변환
    public Product toModel() {
        return Product.builder()
            .id(this.id)
            .sellerId(this.sellerId)
            .categoryId(this.categoryId)
            .brandId(this.brandId)
            .price(this.price)
            .name(this.name)
            .stock(this.stock)
            .createdAt(this.getCreatedAt()) // BaseTimeEntity 필드 포함
            .updatedAt(this.getUpdatedAt()) // BaseTimeEntity 필드 포함
            .deleteAt(this.getDeleteAt()) // BaseTimeEntity 필드 포함
            .build();
    }
}
