package com.sellect.server.payment.event.message;

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
public class OrderCompleteMessage {

    private Payment payment;
    private String token;
    private Long pid;

}
