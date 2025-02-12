package com.sellect.server.category.domain;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Category {

  private final Long id;
  private final String name;
  private final Category parent;
  private final Integer depth;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;
  private final LocalDateTime deleteAt;

  /**
   * 카테고리 등록
   */
  public static Category create(String name, Category parent) {
    return Category.builder()
        .name(name)
        .parent(parent)
        .depth(parent == null ? 0 : parent.getDepth() + 1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .deleteAt(null)
        .build();
  }
}