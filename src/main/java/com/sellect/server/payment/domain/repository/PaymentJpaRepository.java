package com.sellect.server.payment.domain.repository;

import com.sellect.server.payment.domain.repository.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    PaymentEntity findByPid(String pid);

}
