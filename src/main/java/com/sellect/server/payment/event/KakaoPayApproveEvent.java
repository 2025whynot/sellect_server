package com.sellect.server.payment.event;

import com.sellect.server.payment.domain.Payment;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KakaoPayApproveEvent {
    private Payment payment;
    private String token;
    private String pid;

    public static KakaoPayApproveEvent publish(Payment payment, String token, Long pid) {
        return KakaoPayApproveEvent.builder()
            .payment(payment)
            .token(token)
            .pid(String.valueOf(pid))
            .build();
    }

}
