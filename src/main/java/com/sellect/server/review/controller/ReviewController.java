package com.sellect.server.review.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.review.controller.request.ReviewModifyRequest;
import com.sellect.server.review.controller.request.ReviewRegisterRequest;
import com.sellect.server.review.controller.response.ReviewModifyResponse;
import com.sellect.server.review.controller.response.ReviewReadAllResponse;
import com.sellect.server.review.controller.response.ReviewRegisterResponse;
import com.sellect.server.review.domain.Review;
import com.sellect.server.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // todo: 비회원도 조회 가능하도록 Security 에 url 열기
    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<Page<ReviewReadAllResponse>> readAll(
        @PathVariable Long productId,
        @PageableDefault(size = 10, sort = "createdAt", direction = Direction.DESC) Pageable pageable
    ) {
        Page<ReviewReadAllResponse> result = reviewService.readAll(
            productId, pageable);
        return ApiResponse.ok(result);
    }

    @PostMapping("/review")
    public ApiResponse<ReviewRegisterResponse> register(
        @AuthUser User user,
        @Valid @RequestBody ReviewRegisterRequest request
    ) {
        Review review = reviewService.register(user, request);
        return ApiResponse.ok(ReviewRegisterResponse.from(review));
    }

    @PatchMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewModifyResponse> modify(
        @AuthUser User user,
        @PathVariable Long reviewId,
        @Valid @RequestBody ReviewModifyRequest request
    ) {
        Review result = reviewService.modify(user, reviewId, request);
        return ApiResponse.ok(ReviewModifyResponse.from(result));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> remove(
        @AuthUser User user,
        @PathVariable Long reviewId
    ) {
        reviewService.remove(user, reviewId);
        return ApiResponse.ok();
    }
}
