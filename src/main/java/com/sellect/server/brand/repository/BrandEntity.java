package com.sellect.server.brand.repository;

import com.sellect.server.brand.domain.Brand;
import com.sellect.server.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

// todo: AccessLevel 체크 후 사용하세요.
@Entity
@Table(name = "brand")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SuperBuilder
public class BrandEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public static BrandEntity from(Brand brand) {
        return BrandEntity.builder()
            .id(brand.getId())
            .name(brand.getName())
            .createdAt(brand.getCreatedAt())
            .updatedAt(brand.getUpdatedAt())
            .deleteAt(brand.getDeleteAt())
            .build();
    }

    public Brand toModel() {
        return Brand.builder()
            .id(this.id)
            .name(this.name)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .build();
    }
}
