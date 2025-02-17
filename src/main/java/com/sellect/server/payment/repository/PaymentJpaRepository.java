package com.sellect.server.payment.repository;

import com.sellect.server.payment.repository.entity.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    PaymentEntity findByPid(String pid);

    Page<PaymentEntity> findByUid(String uid, Pageable pageable);
}
