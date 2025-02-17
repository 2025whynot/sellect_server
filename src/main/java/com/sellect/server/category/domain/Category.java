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
    // todo: 삭제 예정 -> 직접 DB에 값을 넣으면 되기에 (관리자를 만들지 않기로 기획을 정함)
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