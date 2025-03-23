package com.sellect.server.order.event.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompleteReplyMessage {

    private Long orderId;
    private Boolean orderCompleted;

    public static OrderCompleteReplyMessage onComplete(Long orderId) {
        return OrderCompleteReplyMessage.builder()
            .orderId(orderId)
            .orderCompleted(Boolean.TRUE)
            .build();
    }

    public static OrderCompleteReplyMessage onFail(Long orderId) {
        return OrderCompleteReplyMessage.builder()
            .orderId(orderId)
            .orderCompleted(Boolean.FALSE)
            .build();
    }
}
