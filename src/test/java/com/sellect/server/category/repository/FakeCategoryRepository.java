package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeCategoryRepository implements CategoryRepository {

    private final List<Category> data = new ArrayList<>();

    @Override
    public boolean existsByName(String name) {
        return data.stream()
            .anyMatch(category -> category.getName().equals(name));
    }

    @Override
    public List<Category> findContainingName(String keyword) {
        return data.stream()
            .filter(category -> category.getName().contains(keyword))
            .toList();
    }

    @Override
    public Category findByName(String name) {
        return data.stream()
            .filter(category -> category.getName().equals(name))
            .findFirst().orElse(null);
    }

    public Category save(Category category) {
        findById(category.getId()).ifPresentOrElse(
            existingCategory -> {
                // 기존 데이터 업데이트 (삭제 후 재등록)
                data.remove(existingCategory);
                data.add(category);
            },
            () -> data.add(category) // 새로운 데이터 추가
        );

        return category;
    }

    public Optional<Category> findById(Long categoryId) {
        return data.stream()
            .filter(category -> category.getId().equals(categoryId))
            .filter(category -> category.getDeleteAt() == null)
            .findFirst();
    }

    public List<Category> findAll() {
        return new ArrayList<>(data);
    }

    public void clear() {
        data.clear();
    }
}