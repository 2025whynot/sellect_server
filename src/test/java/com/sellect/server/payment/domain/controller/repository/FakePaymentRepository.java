package com.sellect.server.payment.domain.controller.repository;

import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.repository.PaymentRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

    @Override
    public Page<Payment> findPaymentHistoryByUser(String uuid, Pageable pageable) {
        // 사용자의 모든 결제 내역을 필터링하고 정렬
        List<Payment> filteredPayments = storage.values().stream()
            .filter(payment -> payment.getUid().equals(uuid))
            .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.reverseOrder())) // 최신순으로 정렬
            .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredPayments.size());
        List<Payment> subList = filteredPayments.subList(start, end);
        return new PageImpl<>(subList, pageable, filteredPayments.size());
    }

}
