package com.sellect.server.category.domain;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Category {

    private final Long id;
    private final String name;
    private final Long parentId;
    private final Integer depth;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deleteAt;

    /**
     * 카테고리 등록
     */
    public static Category create(String name, Category parentCategory) {
        return Category.builder()
            .name(name)
            .parentId(parentCategory == null ? null : parentCategory.getParentId())
            .depth(parentCategory == null ? 0 : parentCategory.getDepth() + 1)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deleteAt(null)
            .build();
    }
}