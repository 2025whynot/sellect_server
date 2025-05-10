package com.sellect.server.order.event.message;

import com.sellect.server.payment.domain.Payment;
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
public class PaymentApprovalInitMessage {

    private Long orderId;
    private Long pid;
    private String token;
    private Payment payment;

}
