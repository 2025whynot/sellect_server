package com.sellect.server.payment.event;

import com.sellect.server.payment.domain.Payment;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class PaymentApproveFailedEvent {
    private final Payment payment;
    private final Long pid;
    private final String reason;
}
