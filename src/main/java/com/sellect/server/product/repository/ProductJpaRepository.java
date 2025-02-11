package com.sellect.server.product.repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySellerIdAndNameAndDeleteAt(Long sellerId, String name,
        LocalDateTime deleteAt);

}
