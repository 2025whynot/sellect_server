package com.sellect.server.payment.domain.repository;

import com.sellect.server.payment.domain.Payment;

public interface PaymentRepository {

    void save(Payment payment);

    Payment findByPid(String pid);
}
