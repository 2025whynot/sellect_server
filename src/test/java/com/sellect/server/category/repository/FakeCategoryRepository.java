package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FakeCategoryRepository implements CategoryRepository {

    private final List<Category> data = new ArrayList<>();

    @Override
    public boolean isExistCategory(Long categoryId, LocalDateTime deleteAt) {
        return data.stream()
                .anyMatch(category -> category.getId().equals(categoryId) &&
                        (deleteAt == null || category.getDeleteAt() == null));
    }

    @Override
    public boolean existsByName(String name) {
        return false;
    }

    @Override
    public List<Category> findContainingName(String keyword) {
        return List.of();
    }

    @Override
    public Category findByName(String name) {
        return null;
    }

    public void save(Category category) {
        data.add(category);
    }

    public List<Category> findAll() {
        return new ArrayList<>(data);
    }

    public void clear() {
        data.clear();
    }
}