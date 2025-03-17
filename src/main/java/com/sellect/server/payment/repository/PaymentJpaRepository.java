package com.sellect.server.payment.repository;

import com.sellect.server.payment.repository.entity.PaymentEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByPid(String pid);

    Page<PaymentEntity> findByUid(String uid, Pageable pageable);
    Page<PaymentEntity> findById(Long id, Pageable pageable);
}
