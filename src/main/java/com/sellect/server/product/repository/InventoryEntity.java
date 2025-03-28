package com.sellect.server.product.repository;

import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.product.domain.Inventory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // @Builder 사용 시 명확한 객체 생성
@SuperBuilder
@Entity
@Table(name = "inventory")
public class InventoryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @Column(nullable = false)
    private Integer stock;

//    @Version
//    private Long version; // 낙관적 락용

    public static InventoryEntity from(Inventory inventory) {
        return InventoryEntity.builder()
            .id(inventory.getId())
            .productEntity(ProductEntity.from(inventory.getProduct()))
            .stock(inventory.getStock())
            .createdAt(inventory.getCreatedAt())
            .updatedAt(inventory.getUpdatedAt())
            .deleteAt(inventory.getDeleteAt())
            .build();
    }

    public Inventory toModel() {
        return Inventory.builder()
            .id(this.id)
            .product(this.productEntity.toModel())
            .stock(this.stock)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .build();
    }

    public Inventory updateStock(Integer stock) {
        return Inventory.builder()
            .id(this.id)
            .product(this.productEntity.toModel())
            .stock(stock)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .build();
    }
}
