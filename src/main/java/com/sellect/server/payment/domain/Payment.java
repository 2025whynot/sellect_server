package com.sellect.server.payment.domain;


import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    private Long id;
    private Long pid;
    private Long ordersId;
    private Long userId;
    private Integer price;
    private String tid;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // API 결제 준비 단게
    // 카카오 페이로부터 받아오는 tid 저장 및 상태 저장
    public static Payment ready(Long ordersId, Long pid, Long userId, Integer price, String tid) {
        if (price < 0) {
            throw new CommonException(BError.PAYMENT_FAILED, "결제 금액은 0원 보다 높어야 합니다.");
        }

        if (ordersId == null || pid == null || userId == null || tid == null) {
            throw new CommonException(BError.PAYMENT_FAILED, "결제 정보가 올바르지 않습니다.");
        }

        return Payment.builder()
            .ordersId(ordersId)
            .price(price)
            .userId(userId)
            .pid(pid)
            .tid(tid)
            .status(PaymentStatus.READY)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public Payment approve() {
        if (status == PaymentStatus.READY) {
            return Payment.builder()
                .id(this.id)
                .ordersId(this.ordersId)
                .price(this.price)
                .pid(this.pid)
                .userId(this.userId)
                .status(PaymentStatus.APPROVE)
                .tid(this.tid)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
        }
        return this;
    }

    public Payment failApprove() {
        if (status != PaymentStatus.APPROVE) {
            throw new CommonException(BError.FAIL_FOR_REASON, "Payment.failApprove()", "payment status is not APPROVE");
        }

        return Payment.builder()
            .id(this.id)
            .ordersId(this.ordersId)
            .price(this.price)
            .pid(this.pid)
            .userId(this.userId)
            .status(PaymentStatus.FAIL_APPROVE)
            .tid(this.tid)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public Payment failReady() {
        if (status != PaymentStatus.READY) {
            throw new CommonException(BError.FAIL_FOR_REASON, "Payment.failReady()", "payment status is not READY");
        }
        return Payment.builder()
            .id(this.id)
            .ordersId(this.ordersId)
            .price(this.price)
            .pid(this.pid)
            .userId(this.userId)
            .status(PaymentStatus.FAIL_READY)
            .tid(this.tid)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .build();
    }
}


