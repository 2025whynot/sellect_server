package com.sellect.server.review.service;

import com.sellect.server.auth.domain.User;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import com.sellect.server.review.controller.request.ReviewModifyRequest;
import com.sellect.server.review.controller.request.ReviewRegisterRequest;
import com.sellect.server.review.domain.Review;
import com.sellect.server.review.repository.ReviewEntity;
import com.sellect.server.review.repository.ReviewRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Review register(User user, ReviewRegisterRequest request) {

        // 상품 존재 유무 체크
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new RuntimeException("존재하지 않는 상품입니다."));

        Review review = Review.register(user, product, request.rating(), request.description());

        return reviewRepository.save(ReviewEntity.from(review).toModel());
    }

    @Transactional
    public Review modify(User user, Long reviewId, ReviewModifyRequest request) {
        // 리뷰 존재 유무 체크
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 리뷰입니다."));

        // 유저 권한 확인 (본인의 리뷰만 수정 가능)
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("리뷰 수정 권한이 없습니다.");
        }

        // 수정할 값이 존재할 경우만 수정
        Review modifiedReview = review.modify(
            Optional.ofNullable(request.rating()).orElse(review.getRating()),
            Optional.ofNullable(request.description()).orElse(review.getDescription())
        );

        return reviewRepository.save(modifiedReview);
    }

    @Transactional
    public void remove(User user, Long reviewId) {

        // 리뷰가 존재하는지 체크
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 리뷰입니다."));

        // 유저의 리뷰인지 체크
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("리뷰 삭제 권한이 없습니다.");
        }

        reviewRepository.save(review.remove());
    }
}
