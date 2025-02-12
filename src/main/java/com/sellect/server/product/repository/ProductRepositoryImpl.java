package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository{

    private final ProductJpaRepository productJpaRepository;

    @Override
    public List<Product> saveAll(List<Product> products) {
        List<com.sellect.server.product.repository.ProductEntity> savedEntities = productJpaRepository.saveAll(
            products.stream().map(ProductEntity::from).toList()
        );
        return savedEntities.stream().map(ProductEntity::toModel).toList();
    }

    @Override
    public boolean isDuplicateProduct(Long sellerId, String name, LocalDateTime deleteAt) {
        return productJpaRepository.existsBySellerIdAndNameAndDeleteAt(sellerId, name, null);
    }

    @Override
    public List<Product> findContainingName(String keyword) {
        return productJpaRepository.findContainingName(keyword).stream()
                .map(ProductEntity::toModel)
                .toList();
    }

    @Override
    public List<Product> findByCategoryId(Long categoryId) {
        return productJpaRepository.findByCategoryId(categoryId).stream()
                .map(ProductEntity::toModel)
                .toList();
    }

    @Override
    public List<Product> findByBrandId(Long brandId) {
        return productJpaRepository.findByBrandId(brandId).stream()
                .map(ProductEntity::toModel)
                .toList();
    }

    @Override
    public List<Product> findByIdIn(List<Long> ids) {
        return productJpaRepository.findByIdIn(ids).stream()
                .map(ProductEntity::toModel)
                .toList();
    }
}
