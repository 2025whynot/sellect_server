package com.sellect.server.payment.domain.controller.request;

public record PaymentRequest(
    String orderId,
    String itemName,
    Integer quantity,
    Integer totalAmount
) {

}
