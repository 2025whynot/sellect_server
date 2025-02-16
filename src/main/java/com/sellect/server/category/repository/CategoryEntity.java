package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import com.sellect.server.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
@Entity
@Table(name = "category")
public class CategoryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    private Long parentId;

    private Integer depth;

    public static CategoryEntity from(Category category) {
        return CategoryEntity.builder()
            .id(category.getId())
            .name(category.getName())
            .parentId(category.getParentId())
            .depth(category.getDepth())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .deleteAt(category.getDeleteAt())
            .build();
    }

    public Category toModel() {
        return Category.builder()
            .id(this.id)
            .name(this.name)
            .parentId(this.parentId)
            .depth(this.depth)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .deleteAt(this.getDeleteAt())
            .build();
    }
}