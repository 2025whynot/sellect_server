package com.sellect.server.review.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {

    Optional<ReviewEntity> findByIdAndDeleteAtIsNull(Long reviewId);
}
