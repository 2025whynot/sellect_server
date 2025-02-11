package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    public Optional<Category> findById(Long categoryId, LocalDateTime deleteAt) {
        return categoryJpaRepository.findByIdAndDeleteAt(categoryId, null)
            .map(CategoryEntity::toModel);
    }
}
