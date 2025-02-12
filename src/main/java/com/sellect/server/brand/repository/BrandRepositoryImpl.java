package com.sellect.server.brand.repository;

import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.mapper.BrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;
    private final BrandMapper brandMapper;

    @Override
    public List<Brand> findContainingName(String keyword) {
        return brandJpaRepository.findContainingName(keyword).stream()
                .map(brandMapper::toModel)
                .toList();
    }

    @Override
    public Boolean existsByName(String name) {
        return brandJpaRepository.existsByName(name);
    }
}
