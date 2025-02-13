package com.sellect.server.search.mapper;

import com.sellect.server.product.domain.Product;
import com.sellect.server.search.controller.response.ProductSearchResponse;
import com.sellect.server.search.domain.SearchKeyword;
import com.sellect.server.search.repository.SearchKeywordEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductSearchMapper {

    ProductSearchResponse toProductSearchResponse(Product product);

    SearchKeyword toModel(SearchKeywordEntity entity);

    SearchKeywordEntity toEntity(SearchKeyword model);

}
