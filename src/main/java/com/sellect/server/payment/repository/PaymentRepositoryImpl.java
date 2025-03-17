package com.sellect.server.payment.repository;

import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.entity.PaymentEntity;
import java.util.Optional;
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
    public Optional<Payment> findByPid(String pid) {
        Optional<PaymentEntity> paymentEntity = paymentJpaRepository.findByPid(pid);
        return paymentEntity.map(PaymentEntity::toModel);
    }

//    @Override
//    public Page<Payment> findPaymentHistoryByUser(String uuid, Pageable pageable) {
//        Page<PaymentEntity> paymentEntityPage = paymentJpaRepository.findByUid(uuid, pageable);
//        return paymentEntityPage.map(PaymentEntity::toModel);
//    }

    @Override
    public Page<Payment> findPaymentHistoryByUser(Long userId, Pageable pageable) {
        Page<PaymentEntity> paymentEntityPage = paymentJpaRepository.findById(userId, pageable);
        return paymentEntityPage.map(PaymentEntity::toModel);
    }

}
