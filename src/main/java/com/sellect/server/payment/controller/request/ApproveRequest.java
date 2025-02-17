package com.sellect.server.payment.controller.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public record ApproveRequest(
    String cid,
    String tid,
    String partnerOrderId,
    String partnerUserId,
    String pgToken
) {

}
