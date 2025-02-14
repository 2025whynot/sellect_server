package com.sellect.server.review.repository;

import com.sellect.server.review.domain.Review;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class FakeReviewRepository implements ReviewRepository {

    private final List<Review> data = new ArrayList<>();

    @Override
    public Review save(Review review) {
        findById(review.getId()).ifPresentOrElse(
            existingProduct -> {
                // 기존 데이터 업데이트 (삭제 후 재등록)
                data.remove(existingProduct);
                data.add(review);
            },
            () -> data.add(review) // 새로운 데이터 추가
        );

        return review;
    }

    @Override
    public Optional<Review> findById(Long reviewId) {
        return data.stream()
            .filter(review -> review.getId().equals(reviewId))
            .filter(review -> review.getDeleteAt() == null)
            .findFirst();
    }

    public void clear() {
        data.clear();
    }
}