package com.sellect.server.payment.controller.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;


@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public record KakaoPayReadyRequest(
    String cid,
    String partnerOrderId,
    String partnerUserId,
    String itemName,
    Integer quantity,
    Integer totalAmount,
    Integer taxFreeAmount,
    Integer vatAmount,
    String approvalUrl,
    String cancelUrl,
    String failUrl
) {

}