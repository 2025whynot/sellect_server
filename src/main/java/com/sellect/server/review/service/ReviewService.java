package com.sellect.server.review.service;

import com.sellect.server.auth.domain.User;
import com.sellect.server.product.domain.Product;
import com.sellect.server.product.repository.ProductRepository;
import com.sellect.server.review.controller.request.ReviewRegisterRequest;
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

}
