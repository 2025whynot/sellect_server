package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public List<Product> saveAll(List<Product> products) {
        List<ProductEntity> savedEntities = productJpaRepository.saveAll(
            products.stream().map(ProductEntity::from).toList()
        );
        return savedEntities.stream().map(ProductEntity::toModel).toList();
    }

    @Override
    public boolean isDuplicateProduct(Long sellerId, String name, LocalDateTime deleteAt) {
        return productJpaRepository.existsBySellerIdAndNameAndDeleteAt(sellerId, name, null);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findByIdAndDeleteAt(productId, null)
            .map(ProductEntity::toModel);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(ProductEntity.from(product)).toModel();
    }

    @Override
    public Page<Product> findContainingName(String keyword, Pageable pageable) {
        return productJpaRepository.findContainingName(keyword, pageable)
            .map(ProductEntity::toModel);
    }

    @Override
    public Page<Product> findByCategoryId(Long categoryId, Pageable pageable) {
        return productJpaRepository.findByCategoryId(categoryId, pageable)
            .map(ProductEntity::toModel);
    }

    @Override
    public Page<Product> findByBrandId(Long brandId, Pageable pageable) {
        return productJpaRepository.findByBrandId(brandId, pageable)
            .map(ProductEntity::toModel);
    }

    @Override
    public Page<Product> findByIdIn(List<Long> ids, Pageable pageable) {
        return productJpaRepository.findByIdIn(ids, pageable)
            .map(ProductEntity::toModel);
    }
}
