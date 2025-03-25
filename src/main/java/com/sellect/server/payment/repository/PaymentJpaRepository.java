package com.sellect.server.payment.repository;

import com.sellect.server.payment.domain.PaymentStatus;
import com.sellect.server.payment.repository.entity.PaymentEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByPid(Long pid);

    Page<PaymentEntity> findByUserId(Long userId, Pageable pageable);

    Optional<PaymentEntity> findByPidAndStatus(Long pid, PaymentStatus status);

}
