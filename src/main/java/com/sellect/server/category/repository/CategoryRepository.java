package com.sellect.server.category.repository;

import java.time.LocalDateTime;

public interface CategoryRepository {

    boolean isExistCategory(Long categoryId, LocalDateTime deleteAt);
}
