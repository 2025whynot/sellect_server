package com.sellect.server.auth.repository.entity;

import com.sellect.server.auth.domain.SellerAuth;
import com.sellect.server.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "seller_auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class SellerAuthEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private SellerEntity seller;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    public static SellerAuthEntity from(SellerEntity seller, SellerAuth sellerAuth) {
        return SellerAuthEntity.builder()
                .id(seller.getId())
                .seller(seller)
                .email(sellerAuth.getEmail())
                .password(sellerAuth.getPassword())
                .createdAt(sellerAuth.getCreatedAt())
                .updatedAt(sellerAuth.getUpdatedAt())
                .deleteAt(sellerAuth.getDeleteAt())
                .build();
    }

    public SellerAuth toModel(){
        return SellerAuth.builder()
                .id(this.id)
                .seller(this.seller.toModel())
                .email(this.email)
                .password(this.password)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .deleteAt(this.getDeleteAt())
                .build();
    }
}

