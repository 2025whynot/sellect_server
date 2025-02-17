package com.sellect.server.review.controller.response;

import com.sellect.server.review.domain.Review;

public record ReviewModifyResponse(
    float rating,
    String description
) {

    public static ReviewModifyResponse from(Review review) {
        return new ReviewModifyResponse(
            review.getRating(),
            review.getDescription()
        );
    }
}
