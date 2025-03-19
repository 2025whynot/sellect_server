package com.sellect.server.payment.event;


import com.sellect.server.auth.domain.User;
import com.sellect.server.order.domain.Orders;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class KakaoPayReadyEvent extends ApplicationEvent {

    private final User user;
    private final Orders orders;
    private final CompletableFuture<String> future;

    public KakaoPayReadyEvent(Object source, User user, Orders orders,
        CompletableFuture<String> future) {
        super(source);
        this.user = user;
        this.orders = orders;
        this.future = future;
    }
}
