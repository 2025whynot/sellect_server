package com.sellect.server.auth.repository.entity;


import com.sellect.server.auth.domain.Seller;
import com.sellect.server.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "seller")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class SellerEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String uuid;

    @Column(nullable = false, length = 50)
    private String nickname;


    public static SellerEntity from(Seller seller) {
        return SellerEntity.builder()
                .id(seller.getId())
                .uuid(seller.getUuid())
                .nickname(seller.getNickname())
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .deleteAt(seller.getDeletedAt())
                .build();
    }

    public Seller toModel(){
        return Seller.builder()
                .id(this.id)
                .uuid(this.uuid)
                .nickname(this.nickname)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .deletedAt(this.getDeleteAt())
                .build();
    }
}
