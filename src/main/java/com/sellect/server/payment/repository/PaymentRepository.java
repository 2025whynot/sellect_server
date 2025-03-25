package com.sellect.server.payment.repository;

import com.sellect.server.payment.domain.Payment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByPid(Long pid);

    Page<Payment> findPaymentHistoryByUser(Long userId, Pageable pageable);

    Optional<Payment> findByReadyPid(Long pid);

}
