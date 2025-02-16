package com.sellect.server.review.service;

import com.sellect.server.auth.domain.User;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import com.sellect.server.review.controller.request.ReviewRegisterRequest;
import com.sellect.server.review.controller.request.ReviewRemoveRequest;
import com.sellect.server.review.domain.Review;
import com.sellect.server.review.repository.ReviewEntity;
import com.sellect.server.review.repository.ReviewRepository;
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
        Product product = productRepository.findById(request.produectId())
            .orElseThrow(() -> new RuntimeException("존재하지 않는 상품입니다."));

        Review review = Review.register(user, product, request.rating(), request.description());

        return reviewRepository.save(ReviewEntity.from(review).toModel());
    }

    @Transactional
    public void remove(User user, ReviewRemoveRequest request) {

        // 리뷰가 존재하는지 체크
        Review review = reviewRepository.findById(request.reviewId())
            .orElseThrow(() -> new RuntimeException("존재하지 않는 리뷰입니다."));

        // 유저의 리뷰인지 체크
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("리뷰 삭제 권한이 없습니다.");
        }

        reviewRepository.save(review.remove());
    }
}
