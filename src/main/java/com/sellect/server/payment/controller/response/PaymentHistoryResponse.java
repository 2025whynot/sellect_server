package com.sellect.server.payment.controller.response;

import com.sellect.server.payment.domain.Payment;
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
    public static PaymentHistoryResponse of(Payment payment) {
        return PaymentHistoryResponse.builder()
            .id(payment.getId())
            .orderId(String.valueOf(payment.getOrdersId()))
            .pid(payment.getPid())
            .status(String.valueOf(payment.getStatus()))
            .price(String.valueOf(payment.getPrice()))
            .createdAt(payment.getCreatedAt().toString())
            .build();
    }
}
