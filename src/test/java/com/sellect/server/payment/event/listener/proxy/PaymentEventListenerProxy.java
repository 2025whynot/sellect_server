package com.sellect.server.payment.event.listener.proxy;

import com.sellect.server.order.Infrastructure.port.KakaoPayClient;
import com.sellect.server.order.Infrastructure.response.KakaoPayReadyResponse;
import com.sellect.server.payment.event.KakaoPayApproveEvent;
import com.sellect.server.payment.event.KakaoPayReadyEvent;
import com.sellect.server.payment.event.PaymentEventListener;
import com.sellect.server.payment.repository.PaymentRepository;

public class PaymentEventListenerProxy extends PaymentEventListener {
    public PaymentEventListenerProxy(KakaoPayClient kakaoPayClient, PaymentRepository paymentRepository) {
        super(kakaoPayClient, paymentRepository);
    }

    @Override
    public void kakaoPayReadyEvent(KakaoPayReadyEvent event) {
        super.kakaoPayReadyEvent(event);
    }

    @Override
    public void kakaoPayApproveEvent(KakaoPayApproveEvent event) {
        super.kakaoPayApproveEvent(event);
    }

    @Override
    public void createAndSavePayment(KakaoPayReadyEvent event, Long pid,
        KakaoPayReadyResponse response) {
        super.createAndSavePayment(event, pid, response);
    }
}
