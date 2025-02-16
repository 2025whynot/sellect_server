package com.sellect.server.review.controller;

import com.sellect.server.auth.domain.User;
import com.sellect.server.common.infrastructure.annotation.AuthUser;
import com.sellect.server.common.response.ApiResponse;
import com.sellect.server.review.controller.request.ReviewRegisterRequest;
import com.sellect.server.review.controller.response.ReviewRegisterResponse;
import com.sellect.server.review.domain.Review;
import com.sellect.server.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @PostMapping("/review")
    public ApiResponse<ReviewRegisterResponse> register(
        @AuthUser User user,
        @Valid @RequestBody ReviewRegisterRequest request
    ) {
        Review review = reviewService.register(user, request);
        return ApiResponse.ok(ReviewRegisterResponse.from(review));
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
