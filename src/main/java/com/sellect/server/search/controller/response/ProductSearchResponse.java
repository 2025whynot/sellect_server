package com.sellect.server.search.controller.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductSearchResponse(
    BigDecimal price,
    String name,
    Integer stock,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}