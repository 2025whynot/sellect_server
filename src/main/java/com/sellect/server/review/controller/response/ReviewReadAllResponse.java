package com.sellect.server.review.controller.response;

import com.sellect.server.review.domain.Review;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ReviewReadAllResponse(
    Long reviewId,
    String nickname,
    float rating,
    String description,
    LocalDateTime createdAt
) {

    public static ReviewReadAllResponse from(Review review) {
        return ReviewReadAllResponse.builder()
            .reviewId(review.getId())
            .nickname(review.getUser().getNickname())
            .rating(review.getRating())
            .description(review.getDescription())
            .createdAt(review.getCreatedAt())
            .build();
    }
}
