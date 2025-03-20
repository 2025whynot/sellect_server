package com.sellect.server.payment.domain;


import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
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
    private final Long pid;
    private final Long ordersId;
    private final Long userId;
    private final Integer price;
    private final String tid;
    private final PaymentStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

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

    public Payment approvePayment() {
        if (status.equals(PaymentStatus.READY)) {
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

    public Payment failPayment() {
        if (!status.equals(PaymentStatus.READY)) {
            throw new CommonException(BError.FAIL_FOR_REASON, "failPayment()",
                "PaymentStatus is not Ready");
        }

        return Payment.builder()
            .id(this.id)
            .ordersId(this.ordersId)
            .price(this.price)
            .pid(this.pid)
            .userId(this.userId)
            .status(PaymentStatus.FAIL)
            .tid(this.tid)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public Payment failPreparedPayment() {
        if (status != PaymentStatus.READY) {
            throw new CommonException(BError.NOT_VALID, "결제 상태가 READY가 아님: ");
        }
        return Payment.builder()
            .id(this.id)
            .ordersId(this.ordersId)
            .price(this.price)
            .pid(this.pid)
            .userId(this.userId)
            .status(PaymentStatus.FAIL)
            .tid(this.tid)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .build();
    }


    public Payment cancelPayment() {
        if (!status.equals(PaymentStatus.READY)) {
            throw new CommonException(BError.FAIL_FOR_REASON, "cancelPayment()",
                "PaymentStatus is not Ready");
        }
        return Payment.builder()
            .id(this.id)
            .ordersId(this.ordersId)
            .price(this.price)
            .pid(this.pid)
            .userId(this.userId)
            .status(PaymentStatus.CANCEL)
            .tid(this.tid)
            .createdAt(this.createdAt)
            .updatedAt(LocalDateTime.now())
            .build();
    }
}


