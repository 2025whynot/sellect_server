package com.sellect.server.product.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySellerIdAndNameAndDeleteAt(Long sellerId, String name,
        LocalDateTime deleteAt);

    @Query("SELECT p FROM ProductEntity p where p.name LIKE %:keyword%")
    List<ProductEntity> findContainingName(@Param("keyword") String keyword);

    List<ProductEntity> findByIdIn(List<Long> ids);

}
