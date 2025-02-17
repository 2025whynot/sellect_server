package com.sellect.server.payment.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    @DisplayName("결제 승인")
    void approvePayment() {
        Payment payment = Payment.readyPayment("orderId", "pid", "uid", 1000, "tid");
        Payment approvePayment = payment.approvePayment();
        assertEquals(PaymentStatus.APPROVE, approvePayment.getStatus());
    }
}