package com.sellect.server.payment.event;

import com.sellect.server.order.domain.Orders;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class PaymentPrepareFailedEvent {

    private final Orders orders;
    private final Long pid;
    private final String reason;

}
