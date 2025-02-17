package com.sellect.server.payment.repository;

import com.sellect.server.payment.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepository {

    void save(Payment payment);

    Payment findByPid(String pid);

    Page<Payment> findPaymentHistoryByUser(String uuid, Pageable pageable);
}
