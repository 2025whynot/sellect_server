package com.sellect.server.coupon.repository.entity;

import com.sellect.server.auth.repository.entity.UserEntity;
import com.sellect.server.common.BaseTimeEntity;
import com.sellect.server.coupon.domain.Coupon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class CouponEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity seller;

    @Column(nullable = false)
    private Integer discountCost;

    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    private CouponStatus couponStatus;

    public static CouponEntity from(Coupon coupon) {
        return CouponEntity.builder()
            .id(coupon.getId())
            .seller(UserEntity.from(coupon.getSeller()))
            .discountCost(coupon.getDiscountCost())
            .quantity(coupon.getQuantity())
            .expirationDate(coupon.getExpirationDate())
            .createdAt(coupon.getCreatedAt())
            .updatedAt(coupon.getUpdatedAt())
            .deleteAt(coupon.getDeleteAt())
            .couponStatus(CouponStatus.IN_STOCK)
            .build();
    }

    public Coupon toModel() {
        return Coupon.builder()
            .id(this.id)
            .seller(this.seller.toModel())
            .discountCost(this.discountCost)
            .quantity(this.quantity)
            .expirationDate(this.expirationDate)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .couponStatus(this.couponStatus)
            .build();
    }
}
