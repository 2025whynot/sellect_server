package com.sellect.server.brand.repository;

import com.sellect.server.brand.domain.Brand;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeBrandRepository implements BrandRepository {

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

    public Optional<Brand> findById(Long brandId) {
        return data.stream()
            .filter(brand -> brand.getId().equals(brandId))
            .filter(brand -> brand.getDeleteAt() == null)
            .findFirst();
    }

    public Brand save(Brand brand) {
        findById(brand.getId()).ifPresentOrElse(
            existingBrand -> {
                // 기존 데이터 업데이트 (삭제 후 재등록)
                data.remove(existingBrand);
                data.add(brand);
            },
            () -> data.add(brand) // 새로운 데이터 추가
        );

        return brand;
    }



    public void clear() {
        data.clear();
    }
}
