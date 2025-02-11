package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository{

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
}
