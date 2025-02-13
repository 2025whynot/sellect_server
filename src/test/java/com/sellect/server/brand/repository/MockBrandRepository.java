package com.sellect.server.brand.repository;

import com.sellect.server.brand.domain.Brand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MockBrandRepository implements BrandRepository {

    private final List<Brand> data = new ArrayList<>();

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
