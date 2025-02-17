package com.sellect.server.payment.controller.response;

import lombok.Builder;

@Builder
public record PaymentHistoryResponse(
    Long id,
    String price,
    String orderId,
    String pid,
    String status,
    String createdAt
) {

}
