package com.sellect.server.product.repository;

import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.brand.repository.BrandEntity;
import com.sellect.server.category.repository.CategoryEntity;
import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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

    // todo: 관계 체크
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id") // ERD 기준
    private UserEntity sellerEntity;

    // todo: 관계 체크
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity categoryEntity;

    // todo: 관계 체크
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private BrandEntity brandEntity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer stock;


    public static ProductEntity from(Product product) {
        return ProductEntity.builder()
            .id(product.getId())
            .sellerEntity(UserEntity.from(product.getSeller()))
            .categoryEntity(CategoryEntity.from(product.getCategory()))
            .brandEntity(BrandEntity.from(product.getBrand()))
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
            .seller(this.sellerEntity.toModel())
            .category(this.categoryEntity.toModel())
            .brand(this.brandEntity.toModel())
            .price(this.price)
            .name(this.name)
            .stock(this.stock)
            .createdAt(this.getCreatedAt()) // BaseTimeEntity 필드 포함
            .updatedAt(this.getUpdatedAt()) // BaseTimeEntity 필드 포함
            .deleteAt(this.getDeleteAt()) // BaseTimeEntity 필드 포함
            .build();
    }
}
