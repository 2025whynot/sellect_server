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
    private final Product product;
    private final BigDecimal price;
    private final int quantity;
    private final LocalDateTime createdAt;
    private final LocalDateTime deleteAt;

    // 주문 상품 생성
    public static OrderItem register(Orders orders, Product product, BigDecimal price,
        int quantity) {
        return OrderItem.builder()
            .orders(orders)
            .product(product)
            .price(price)
            .quantity(quantity)
            .createdAt(LocalDateTime.now())
            .deleteAt(null)
            .build();
    }

    // 재고 확인 및 차감
    // todo: 이게 머임? <- 설명 좀...
    public Inventory deductStock(Inventory inventory) {
        return inventory.deductStock(this.quantity);
    }
}