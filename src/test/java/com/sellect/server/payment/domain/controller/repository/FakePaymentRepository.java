package com.sellect.server.payment.domain.controller.repository;

import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.domain.repository.PaymentRepository;
import java.util.HashMap;
import java.util.Map;

public class FakePaymentRepository implements PaymentRepository {

    private final Map<Long, Payment> storage = new HashMap<>();
    private long id = 1L;

    @Override
    public void save(Payment payment) {
        if (payment.getId() == null) {
            payment = Payment.builder()
                .id(id++)
                .orderId(payment.getOrderId())
                .price(payment.getPrice())
                .uid(payment.getUid())
                .pid(payment.getPid())
                .tid(payment.getTid())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
        }
        storage.put(payment.getId(), payment);
    }

    @Override
    public Payment findByPid(String pid) {
        return storage.values().stream()
            .filter(payment -> payment.getPid().equals(pid))
            .findFirst()
            .orElse(null);
    }
}
