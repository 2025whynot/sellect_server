package com.sellect.server.payment.repository.entity;

import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.domain.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class PaymentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pid;

    //    private String uid;
    private Long userId;

    @JoinColumn(name = "orders_id")
    private Long ordersId;

    private Integer price;

    private String tid;
    @Enumerated(value = EnumType.STRING)

    private PaymentStatus status;

    public static PaymentEntity from(Payment payment) {
        return PaymentEntity.builder()
            .id(payment.getId())
            .pid(payment.getPid())
//            .uid(payment.getUid())
            .userId(payment.getUserId())
            .ordersId(payment.getOrdersId())
            .price(payment.getPrice())
            .tid(payment.getTid())
            .status(payment.getStatus())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }


    public Payment toModel() {
        return Payment.builder()
            .id(this.id)
            .pid(this.pid)
//            .uid(this.uid)
            .userId(this.userId)
            .ordersId(this.ordersId)
            .price(this.price)
            .tid(this.tid)
            .status(this.status)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }
}
