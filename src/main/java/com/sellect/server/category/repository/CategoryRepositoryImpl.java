package com.sellect.server.category.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.sellect.server.category.domain.Category;
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

    @Override
    public boolean existsByName(String name) {
        return categoryJpaRepository.existsByName(name);
    }

    @Override
    public List<Category> findContainingName(String keyword) {
        return categoryJpaRepository.findContainingName(keyword);
    }

}
