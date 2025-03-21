package com.sellect.server.order.Infrastructure.message;

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
public class PayReadyMessage {

    private Long userId;
    private Long orderId;
    private int totalPrice;

}
