package com.sellect.server.review.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReviewRegisterRequest(
    @NotNull(message = "상품 ID는 필수입니다.")
    Long productId,

    @Min(value = 0, message = "평점은 0 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5 이하여야 합니다.")
    float rating,

    @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Size(min = 10, message = "리뷰 내용은 최소 10자 이상이어야 합니다.")
    String description
) {

}
