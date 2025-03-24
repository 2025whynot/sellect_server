package com.sellect.server.payment.event.listener.proxy;

import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.payment.event.KakaoPayApproveEvent;
import com.sellect.server.payment.event.KakaoPayReadyEvent;
import com.sellect.server.payment.event.listener.PaymentEventListener;
import com.sellect.server.payment.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;

public class PaymentEventListenerProxy extends PaymentEventListener {
    public PaymentEventListenerProxy(KakaoPayClient kakaoPayClient, PaymentRepository paymentRepository, ApplicationEventPublisher eventPublisher) {
        super(kakaoPayClient, paymentRepository, eventPublisher);
    }

    @Override
    public void kakaoPayReadyEvent(KakaoPayReadyEvent event) {
        super.kakaoPayReadyEvent(event);
    }

    @Override
    public void kakaoPayApproveEvent(KakaoPayApproveEvent event) {
        super.kakaoPayApproveEvent(event);
    }
}
