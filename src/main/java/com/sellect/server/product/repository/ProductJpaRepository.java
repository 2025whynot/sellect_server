package com.sellect.server.product.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySellerIdAndNameAndDeleteAt(Long sellerId, String name,
        LocalDateTime deleteAt);

    Optional<ProductEntity> findByIdAndDeleteAt(Long productId, LocalDateTime deleteAt);

}
