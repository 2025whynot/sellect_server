package com.sellect.server.brand.mapper;

import com.sellect.server.brand.domain.Brand;
import com.sellect.server.brand.repository.BrandEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrandMapper {

  Brand toModel(BrandEntity entity);

  BrandEntity toEntity(Brand model);
}
