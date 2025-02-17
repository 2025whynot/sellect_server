package com.sellect.server.review.controller.response;

import com.sellect.server.review.domain.Review;

public record ReviewRegisterResponse(
    Long reviewId
) {
    public static ReviewRegisterResponse from(Review review) {
        return new ReviewRegisterResponse(
            review.getId()
        );
    }
}
