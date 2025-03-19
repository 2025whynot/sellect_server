package com.sellect.server.order.repository.entity;

import com.sellect.server.order.domain.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "order_item")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id")
    private OrdersEntity ordersEntity;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "product_id")
//    private ProductEntity productEntity;
    @JoinColumn(name = "product_id")
    private Long productId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deleteAt;

    public OrderItem toModel() {
        return OrderItem.builder()
            .id(this.id)
            .orders(this.ordersEntity.toModel())
//            .product(this.productEntity.toModel())
            .productId(this.productId)
            .price(this.price)
            .quantity(this.quantity)
            .createdAt(this.createdAt)
            .deleteAt(this.deleteAt)
            .build();
    }

    public static OrderItemEntity from(OrderItem orderItem) {
        return OrderItemEntity.builder()
            .id(orderItem.getId())
            .ordersEntity(OrdersEntity.from(orderItem.getOrders()))
//            .productEntity(ProductEntity.from(orderItem.getProduct()))
            .productId(orderItem.getProductId())
            .price(orderItem.getPrice())
            .quantity(orderItem.getQuantity())
            .createdAt(orderItem.getCreatedAt())
            .deleteAt(orderItem.getDeleteAt())
            .build();
    }
}
