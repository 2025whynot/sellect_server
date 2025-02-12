package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;

import java.time.LocalDateTime;
import java.util.List;

public interface CategoryRepository {

  boolean isExistCategory(Long categoryId, LocalDateTime deleteAt);

  boolean existsByName(String name);

  List<Category> findContainingName(String keyword);

  Category findByName(String name);
}
