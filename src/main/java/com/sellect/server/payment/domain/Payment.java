package com.sellect.server.payment.domain;


import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    private final Long id;
    private final String pid;
    private final String orderId;
    private final String uid;
    private final Integer price;
    private final String tid;
    private final PaymentStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // API 결제 준비 단게
    // 카카오 페이로부터 받아오는 tid 저장 및 상태 저장
    public static Payment readyPayment(String orderId, String pid, String uid, Integer price,
        String tid) {
        return Payment.builder()
            .orderId(orderId)
            .price(price)
            .uid(uid)
            .pid(pid)
            .tid(tid)
            .status(PaymentStatus.READY)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public Payment approvePayment() {
        return Payment.builder()
            .id(this.id)
            .orderId(this.orderId)
            .price(this.price)
            .pid(this.pid)
            .uid(this.uid)
            .status(PaymentStatus.APPROVE)
            .tid(this.tid)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .build();
    }

}


