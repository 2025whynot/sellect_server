package com.sellect.server.brand.repository;

import com.sellect.server.brand.domain.Brand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeBrandRepository implements BrandRepository {

    private final List<Brand> data = new ArrayList<>();

    // todo: 테스트 작성 시 구현해야함
    @Override
    public Optional<Brand> findById(Long brandId) {
        return Optional.empty();
    }

    @Override
    public Brand findByName(String name) {
        return data.stream()
            .filter(brand -> brand.getName().equals(name))
            .findFirst().orElse(null);
    }

    @Override
    public List<Brand> findContainingName(String keyword) {
        return data.stream()
            .filter(brand -> brand.getName().contains(keyword))
            .collect(Collectors.toList());
    }

    @Override
    public Boolean existsByName(String name) {
        return data.stream()
            .anyMatch(brand -> brand.getName().equals(name));
    }

    public void save(Brand brand) {
        data.add(brand);
    }

    public void clear() {
        data.clear();
    }
}
