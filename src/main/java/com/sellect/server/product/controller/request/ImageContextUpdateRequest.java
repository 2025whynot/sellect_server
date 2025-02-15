package com.sellect.server.product.controller.request;

import lombok.Builder;

@Builder
public record ImageContextUpdateRequest(
    String target,
    String prev,
    String next,
    boolean isRepresentative,
    boolean isNewImage
) {

}
