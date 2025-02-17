package com.sellect.server.product.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySellerEntityIdAndNameAndDeleteAtIsNull(Long sellerId, String name);

    @Query("SELECT p FROM ProductEntity p where p.name LIKE %:keyword%")
    Page<ProductEntity> findContainingName(@Param("keyword") String keyword, Pageable pageable);

    Optional<ProductEntity> findByIdAndDeleteAtIsNull(Long productId);

}
