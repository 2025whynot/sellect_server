package com.sellect.server.payment.repository;

import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.entity.PaymentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public void save(Payment payment) {
        PaymentEntity paymentEntity = PaymentEntity.from(payment);
        paymentJpaRepository.save(paymentEntity);
    }

    @Override
    public Payment findByPid(String pid) {
        PaymentEntity paymentEntity = paymentJpaRepository.findByPid(pid);
        return paymentEntity.toModel();
    }

    @Override
    public Page<Payment> findPaymentHistoryByUser(String uuid, Pageable pageable) {
        Page<PaymentEntity> paymentEntityPage = paymentJpaRepository.findByUid(uuid, pageable);
        return paymentEntityPage.map(PaymentEntity::toModel);
    }
}
