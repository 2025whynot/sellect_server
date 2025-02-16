package com.sellect.server.review.repository;

import com.sellect.server.review.domain.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepository {

    Optional<Review> findById(Long reviewId);

    Page<Review> findAllByProductId(Long productId, Pageable pageable);

    Review save(Review review);
}
