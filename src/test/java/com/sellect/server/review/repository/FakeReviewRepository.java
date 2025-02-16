package com.sellect.server.review.repository;

import com.sellect.server.review.domain.Review;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public class FakeReviewRepository implements ReviewRepository {

    private long idSequence = 1L; // 자동 증가 ID 변수 추가
    private final List<Review> data = new ArrayList<>();

    @Override
    public Review save(Review review) {
        // 리뷰 ID가 없으면 새로운 ID를 자동 할당
        if (review.getId() == null) {
            review = Review.builder()
                .id(idSequence++)  // 자동 증가 ID 부여
                .user(review.getUser())
                .product(review.getProduct())
                .rating(review.getRating())
                .description(review.getDescription())
                .build();
        }

        // 기존 리뷰가 존재하는지 확인하고 업데이트
        Optional<Review> existingReview = findById(review.getId());
        existingReview.ifPresent(data::remove); // 존재하면 삭제
        data.add(review); // 새로 추가

        return review;
    }

    @Override
    public Optional<Review> findById(Long reviewId) {
        return data.stream()
            .filter(review -> review.getId().equals(reviewId))
            .filter(review -> review.getDeleteAt() == null)
            .findFirst();
    }

    // todo: 테스트 구현 시 작성
    @Override
    public Page<Review> findAllByProductId(Long productId, Pageable pageable) {
        return null;
    }

    public void clear() {
        data.clear();
    }
}