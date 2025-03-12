package com.sellect.server.product.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySellerEntityIdAndNameAndDeleteAtIsNull(Long sellerId, String name);

    @Query("SELECT p FROM ProductEntity p where p.name LIKE %:keyword%")
    Page<ProductEntity> findContainingName(@Param("keyword") String keyword, Pageable pageable);

    Optional<ProductEntity> findByIdAndDeleteAtIsNull(Long productId);

    // 판매자 아이디로 상품 조회
    Page<ProductEntity> findBySellerEntityId(Long sellerId, Pageable pageable);

    List<Long> findIdBySellerEntityIdAndDeleteAtIsNull(Long sellerId);

    // payment approve v0
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    Optional<ProductEntity> findWithLockById(@Param("id") Long id);
}
