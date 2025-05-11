package com.sellect.server.payment.repository; // 사용자의 패키지 경로

import com.sellect.server.payment.domain.Payment;
import com.sellect.server.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class FakePaymentRepository implements PaymentRepository {

    private final Map<Long, Payment> storage = new HashMap<>();
    private long idSequence = 1L;

    // --- 예외 발생 제어를 위한 필드 추가 ---
    private Long triggerSaveFailureForPaymentId = null;
    private PaymentStatus triggerSaveFailureForPaymentStatus = null;
    private RuntimeException exceptionToThrowOnSave = null;
    // --- 예외 발생 제어를 위한 필드 끝 ---

    /**
     * 다음에 save 메소드가 특정 조건의 Payment 객체와 함께 호출될 때 지정된 예외를 발생시키도록 설정합니다.
     *
     * @param paymentId 대상 Payment의 ID
     * @param status  대상 Payment의 상태 (이 상태의 Payment가 저장되려고 할 때 예외 발생)
     * @param ex      발생시킬 RuntimeException
     */
    public void setNextSaveToFail(Long paymentId, PaymentStatus status, RuntimeException ex) {
        this.triggerSaveFailureForPaymentId = paymentId;
        this.triggerSaveFailureForPaymentStatus = status;
        this.exceptionToThrowOnSave = ex;
    }

    @Override
    public Payment save(Payment paymentInput) {
        // --- 예외 발생 조건 체크 로직 추가 ---
        if (triggerSaveFailureForPaymentId != null &&
                Objects.equals(triggerSaveFailureForPaymentId, paymentInput.getId()) &&
                (triggerSaveFailureForPaymentStatus == null || triggerSaveFailureForPaymentStatus == paymentInput.getStatus()) &&
                exceptionToThrowOnSave != null) {

            RuntimeException exToThrow = this.exceptionToThrowOnSave;
            // 다음 save 호출에 영향을 주지 않도록 조건 초기화
            this.triggerSaveFailureForPaymentId = null;
            this.triggerSaveFailureForPaymentStatus = null;
            this.exceptionToThrowOnSave = null;
            throw exToThrow;
        }
        // --- 예외 발생 조건 체크 로직 끝 ---

        Payment paymentToStore;
        if (paymentInput.getId() == null) {
            paymentToStore = Payment.builder()
                    .id(idSequence++) // 새 ID 할당
                    .pid(paymentInput.getPid())
                    .ordersId(paymentInput.getOrdersId())
                    .userId(paymentInput.getUserId())
                    .price(paymentInput.getPrice())
                    .tid(paymentInput.getTid())
                    .status(paymentInput.getStatus())
                    .createdAt(paymentInput.getCreatedAt())
                    .updatedAt(paymentInput.getUpdatedAt())
                    .build();
        } else {
            paymentToStore = Payment.builder()
                    .id(paymentInput.getId())
                    .pid(paymentInput.getPid())
                    .ordersId(paymentInput.getOrdersId())
                    .userId(paymentInput.getUserId())
                    .price(paymentInput.getPrice())
                    .tid(paymentInput.getTid())
                    .status(paymentInput.getStatus())
                    .createdAt(paymentInput.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        storage.put(paymentToStore.getId(), paymentToStore);
        return paymentToStore;
    }

    @Override
    public Optional<Payment> findByPid(Long pid) {
        return storage.values().stream()
                .filter(payment -> payment.getPid() != null && payment.getPid().equals(pid))
                .findFirst();
    }

    @Override
    public Page<Payment> findPaymentHistoryByUser(Long userId, Pageable pageable) {
        List<Payment> filteredPayments = storage.values().stream()
                .filter(payment -> payment.getUserId() != null && payment.getUserId().equals(userId))
                .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredPayments.size());

        if (start >= end) {
            return new PageImpl<>(new ArrayList<>(), pageable, filteredPayments.size());
        }
        return new PageImpl<>(filteredPayments.subList(start, end), pageable, filteredPayments.size());
    }

    @Override
    public Optional<Payment> findByReadyPid(Long pid) {
        return storage.values().stream()
                .filter(payment -> payment.getPid() != null && payment.getPid().equals(pid) && payment.getStatus() == PaymentStatus.READY)
                .findFirst();
    }

    public void clear() {
        storage.clear();
        idSequence = 1L;
        this.triggerSaveFailureForPaymentId = null;
        this.triggerSaveFailureForPaymentStatus = null;
        this.exceptionToThrowOnSave = null;
    }

    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }
}