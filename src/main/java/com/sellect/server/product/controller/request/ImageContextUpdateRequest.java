package com.sellect.server.product.controller.request;

import lombok.Getter;

public record ImageContextUpdateRequest(
    String sourceUUID,
    Direction direction,
    String targetUUID
) {

    public boolean isPrev() {
        return direction == Direction.PREV;
    }

    public boolean isNext() {
        return direction == Direction.NEXT;
    }

    @Getter
    enum Direction {
        PREV("prev"), NEXT("next");

        private final String value;

        Direction(String value) {
            this.value = value;
        }
    }

}
