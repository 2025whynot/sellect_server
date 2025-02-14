package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductImage;
import java.util.Optional;

public interface ProductImageRepository {

    void save(ProductImage productImage, Product product);

    Optional<ProductImage> findByProductIdAndUuid(Long productId, String uuid);

}
