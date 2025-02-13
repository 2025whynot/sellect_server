package com.sellect.server.category.repository;

import com.sellect.server.category.domain.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeCategoryRepository implements CategoryRepository {

    private final List<Category> data = new ArrayList<>();

    // todo : 테스트 작성 시 구현해야 함
    @Override
    public Optional<Category> findById(Long categoryId) {
        return Optional.empty();
    }

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