package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findById(Long categoryId, LocalDateTime deleteAt);
}
