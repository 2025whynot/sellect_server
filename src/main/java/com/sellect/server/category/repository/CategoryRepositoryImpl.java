package com.sellect.server.category.repository;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public boolean isExistCategory(Long categoryId, LocalDateTime deleteAt) {
        return categoryJpaRepository.existsByIdAndDeleteAt(categoryId, null);
    }
}
