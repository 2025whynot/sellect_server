package com.sellect.server.brand.repository;

import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.mapper.BrandMapper;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;
    private final BrandMapper brandMapper;

    @Override
    public Optional<Brand> findById(Long brandId) {
        return brandJpaRepository.findByIdAndDeleteAtIsNull(brandId)
            .map(BrandEntity::toModel);
    }

    @Override
    public Brand findByName(String name) {
        BrandEntity brandEntity = brandJpaRepository.findByName(name)
            .orElseThrow(() -> new CommonException(BError.NOT_EXIST, "brand"));
        return brandMapper.toModel(brandEntity);
    }

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
