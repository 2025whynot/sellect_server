package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findById(Long categoryId);
}
