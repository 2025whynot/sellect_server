package com.sellect.server.tmp.mapper;

import com.sellect.server.tmp.dto.response.ProductSearchResponse;
import com.sellect.server.tmp.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductSearchMapper {

    ProductSearchResponse toProductSearchResponse(Product product);
}
