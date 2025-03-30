package com.sellect.server.payment.event;

import com.sellect.server.order.domain.OrderItem;
import com.sellect.server.payment.domain.Payment;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KakaoPayApproveRedisEvent {
    private List<OrderItem> orderItems;
    private Payment payment;
    private String token;
    private Long pid;

    public static KakaoPayApproveRedisEvent publish(List<OrderItem> orderItems, Payment payment, String token, Long pid) {
        return KakaoPayApproveRedisEvent.builder()
            .orderItems(orderItems)
            .payment(payment)
            .token(token)
            .pid(pid)
            .build();
    }

}
