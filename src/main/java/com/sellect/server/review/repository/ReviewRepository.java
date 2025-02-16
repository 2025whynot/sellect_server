package com.sellect.server.review.repository;

import com.sellect.server.review.domain.Review;
import java.util.Optional;

public interface ReviewRepository {

    Optional<Review> findById(Long reviewId);

    Review save(Review reivew);
}
