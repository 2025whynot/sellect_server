package com.sellect.server.tmp.dto.response;

public record ProductSearchResponse(
        Long id,
        Float price,
        String name,
        Integer stock
) {
}
