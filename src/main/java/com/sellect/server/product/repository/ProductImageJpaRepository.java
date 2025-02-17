package com.sellect.server.product.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageJpaRepository extends JpaRepository<ProductImageEntity, Long> {

    @Query("SELECT p FROM ProductImageEntity p "
        + "WHERE p.productEntity.id = :productId "
        + "AND p.uuid = :uuid "
        + "AND p.deleteAt IS NULL")
    Optional<ProductImageEntity> findByProductIdAndUuid(
        @Param("productId") Long productId,
        @Param("uuid") String uuid);

    @Query("SELECT p FROM ProductImageEntity p "
        + "WHERE p.productEntity.id = :productId "
        + "AND p.deleteAt IS NULL")
    List<ProductImageEntity> findByProductId(@Param("productId") Long productId);
}
