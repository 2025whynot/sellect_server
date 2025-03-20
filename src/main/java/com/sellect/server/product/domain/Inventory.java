package com.sellect.server.product.domain;

import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Inventory {

    private final Long id;
    private final Product product;
    private final int stock;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    public static Inventory register(Product product, int stock) {
        return Inventory.builder()
            .product(product)
            .stock(stock)
            .build();
    }

    public Inventory modifyStock(int stock) {
        return Inventory.builder()
            .id(this.id)
            .product(this.product)
            .stock(stock)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    // 재고 부족 확인
    public void validateStock(int quantity) {
        if (this.stock < quantity) {
            throw new CommonException(BError.NOT_VALID, "재고 부족");
        }
    }

    // 재고 차감
    public Inventory deductStock(int quantity) {
        // 재고 확인
        validateStock(quantity);
        return Inventory.builder()
            .id(this.id)
            .product(this.product)
            .stock(this.stock - quantity)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }

    public Inventory deleteStock() {
        return Inventory.builder()
            .id(this.id)
            .product(this.product)
            .stock(0)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .deleteAt(LocalDateTime.now())
            .build();
    }

    // 차감된 재고 복구
    public Inventory restoreStock(int quantity) {
        return Inventory.builder()
            .id(this.id)
            .product(this.product)
            .stock(this.stock + quantity)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .deleteAt(this.deleteAt)
            .build();
    }
}
