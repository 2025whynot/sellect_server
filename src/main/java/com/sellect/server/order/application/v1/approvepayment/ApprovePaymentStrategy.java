package com.sellect.server.order.application.v1.approvepayment;

public interface ApprovePaymentStrategy {

    void approvePayment(final Long pid, final String token);
}
