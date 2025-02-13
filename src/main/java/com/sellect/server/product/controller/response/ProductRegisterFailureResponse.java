package com.sellect.server.product.controller.response;

public record ProductRegisterFailureResponse(
    String name,
    String reason
) {

    public static ProductRegisterFailureResponse from(String name, String reason) {
        return new ProductRegisterFailureResponse(name, reason);
    }
}