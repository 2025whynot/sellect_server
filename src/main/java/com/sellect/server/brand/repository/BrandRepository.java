package com.sellect.server.brand.repository;

import com.sellect.server.brand.domain.Brand;

import java.util.List;

public interface BrandRepository {

    List<Brand> findContainingName(String keyword);

    Boolean existsByName(String name);
}
