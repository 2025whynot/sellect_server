package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import com.sellect.server.product.domain.ProductImage;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductImageRepositoryImpl implements ProductImageRepository {

    private final ProductImageJpaRepository productImageJpaRepository;

    @Override
    public void save(ProductImage productImage, Product product) {
        productImageJpaRepository.save(ProductImageEntity.from(productImage, product));
    }

    @Override
    public Optional<ProductImage> findByProductIdAndUuid(Long productId, String uuid) {
        return productImageJpaRepository.findByProductIdAndUuid(productId, uuid)
            .map(ProductImageEntity::toModel);
    }

}
