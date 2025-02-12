package com.sellect.server.product.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySellerIdAndNameAndDeleteAt(Long sellerId, String name,
        LocalDateTime deleteAt);

    @Query("SELECT p FROM ProductEntity p where p.name LIKE %:keyword%")
    Page<ProductEntity> findContainingName(@Param("keyword") String keyword, Pageable pageable);

    Page<ProductEntity> findByCategoryId(Long categoryId, Pageable pageable);

    Page<ProductEntity> findByBrandId(Long brandId, Pageable pageable);

    Page<ProductEntity> findByIdIn(List<Long> ids, Pageable pageable);
  
    Optional<ProductEntity> findByIdAndDeleteAt(Long productId, LocalDateTime deleteAt);

}
