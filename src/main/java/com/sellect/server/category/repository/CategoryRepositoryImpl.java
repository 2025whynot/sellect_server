package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public Optional<Category> findById(Long categoryId) {
        return categoryJpaRepository.findByIdAndDeleteAtIsNull(categoryId)
            .map(CategoryEntity::toModel);
    }

}
