package com.sellect.server.payment.event.message;

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
    private Long userReceivedCouponId;
    private int totalPrice;

}
