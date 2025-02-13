package com.sellect.server.category.mapper;

import com.sellect.server.category.domain.Category;
import com.sellect.server.category.repository.CategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toModel(CategoryEntity entity);

    CategoryEntity toEntity(Category model);
}
