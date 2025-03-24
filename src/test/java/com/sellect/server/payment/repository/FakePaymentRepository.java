package com.sellect.server.payment.repository;

import com.sellect.server.payment.domain.Payment;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class FakePaymentRepository implements PaymentRepository {

    private final Map<Long, Payment> storage = new HashMap<>();
    private long id = 1L;

    @Override
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            payment = Payment.builder()
                .id(id++)
                .ordersId(payment.getOrdersId())
                .price(payment.getPrice())
//                .uid(payment.getUid())
                .userId(payment.getUserId())
                .pid(payment.getPid())
                .tid(payment.getTid())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
        }
        storage.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findByPid(Long pid) {
        return storage.values().stream()
            .filter(payment -> payment.getPid().equals(pid))
            .findFirst();
    }

    @Override
//    public Page<Payment> findPaymentHistoryByUser(String uuid, Pageable pageable) {
    public Page<Payment> findPaymentHistoryByUser(Long userId, Pageable pageable) {
        // 사용자의 모든 결제 내역을 필터링하고 정렬
        List<Payment> filteredPayments = storage.values().stream()
//            .filter(payment -> payment.getUid().equals(uuid))
            .filter(payment -> payment.getUserId().equals(userId))
            .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.reverseOrder())) // 최신순으로 정렬
            .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredPayments.size());
        List<Payment> subList = filteredPayments.subList(start, end);
        return new PageImpl<>(subList, pageable, filteredPayments.size());
    }

    // todo: 테스트 코드 작성 시 구현
    @Override
    public Optional<Payment> findByReadyPid(Long pid) {
        return Optional.empty();
    }

}
