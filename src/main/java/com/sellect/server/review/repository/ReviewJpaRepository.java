package com.sellect.server.review.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {

    Optional<ReviewEntity> findByIdAndDeleteAtIsNull(Long reviewId);

    // todo: 한번 더 체크해볼 것
    @EntityGraph(attributePaths = {"userEntity", "productEntity"})
    Page<ReviewEntity> findAllByProductEntityId(Long productId, Pageable pageable);
}
