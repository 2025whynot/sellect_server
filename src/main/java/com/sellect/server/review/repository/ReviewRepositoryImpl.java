package com.sellect.server.review.repository;

import com.sellect.server.review.domain.Review;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

    private final ReviewJpaRepository reviewJpaRepository;

    @Override
    public Optional<Review> findById(Long reviewId) {
        return reviewJpaRepository.findByIdAndDeleteAtIsNull(reviewId)
            .map(ReviewEntity::toModel);
    }

    // todo : N+1 발생 예상
    @Override
    public Page<Review> findAllByProductId(Long productId, Pageable pageable) {
        return reviewJpaRepository.findAllByProductEntityId(productId, pageable)
            .map(ReviewEntity::toModel);
    }

    @Override
    public Review save(Review reivew) {
        return reviewJpaRepository.save(ReviewEntity.from(reivew)).toModel();
    }

}
