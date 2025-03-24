package com.sellect.server.order.repository.entity;

import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.coupon.repository.entity.UserReceivedCouponEntity;
import com.sellect.server.order.domain.Orders;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "orders")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrdersEntity extends BaseTimeEntity {

    @Id
    @Tsid
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_received_coupon_id")
    private UserReceivedCouponEntity userReceivedCouponEntity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

//    @OneToMany(mappedBy = "orderEntity", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<OrderItemEntity> orderItems; // 추가

    public Orders toModel() {
        return Orders.builder()
            .id(this.id)
            .user(this.userEntity.toModel())
            .userReceivedCoupon(
                this.userReceivedCouponEntity != null ?
                    this.userReceivedCouponEntity.toModel() : null)
            .totalPrice(this.totalPrice)
            .orderNumber(this.orderNumber)
            .status(this.status)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .build();
    }

    public static OrdersEntity from(Orders orders) {
        return OrdersEntity.builder()
            .id(orders.getId())
            .userEntity(UserEntity.from(orders.getUser()))
            .userReceivedCouponEntity(
                orders.getUserReceivedCoupon() != null ?
                    UserReceivedCouponEntity.from(orders.getUserReceivedCoupon()) : null)
            .totalPrice(orders.getTotalPrice())
            .orderNumber(orders.getOrderNumber())
            .status(orders.getStatus())
            .createdAt(orders.getCreatedAt())
            .updatedAt(orders.getUpdatedAt())
            .deleteAt(orders.getDeleteAt())
            .build();
    }
}
