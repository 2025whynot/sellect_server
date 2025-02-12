package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import com.sellect.server.category.mapper.CategoryMapper;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

  private final CategoryJpaRepository categoryJpaRepository;
  private final CategoryMapper categoryMapper;

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
    return categoryJpaRepository.findContainingName(keyword).stream()
        .map(categoryMapper::toModel)
        .toList();
  }

  @Override
  public Category findByName(String name) {
    CategoryEntity categoryEntity = categoryJpaRepository.findByName(name)
        .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "category name"));
    return categoryMapper.toModel(categoryEntity);
  }

}
