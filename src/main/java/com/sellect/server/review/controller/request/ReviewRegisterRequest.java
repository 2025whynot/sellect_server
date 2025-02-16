package com.sellect.server.review.controller.request;

import lombok.Builder;

@Builder
public record ReviewRegisterRequest(
    Long produectId,
    float rating,
    String description
) {

}
