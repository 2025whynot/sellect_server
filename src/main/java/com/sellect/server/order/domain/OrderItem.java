package com.sellect.server.order.domain;

import com.sellect.server.product.domain.Inventory;
import com.sellect.server.product.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    private final Long id;
    private final Orders orders;
    private final Long productId;
    private final BigDecimal price;
    private final int quantity;
    private final LocalDateTime createdAt;
    private final LocalDateTime deleteAt;

    // 주문 상품 생성
    public static OrderItem register(Orders orders, Product product, BigDecimal price,
        int quantity) {
        // TODO: Product.deletedAt Null 체크

        return OrderItem.builder()
            .orders(orders)
            .productId(product.getId())
            .price(price)
            .quantity(quantity)
            .createdAt(LocalDateTime.now())
            .deleteAt(null)
            .build();
    }

    // 재고 확인 및 차감
    // V1After부터는 안 씀 (Legacy V0)
    public Inventory deductStock(Inventory inventory) {
        return inventory.deductStock(this.quantity);
    }

}