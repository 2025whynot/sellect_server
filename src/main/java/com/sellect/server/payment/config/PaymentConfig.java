package com.sellect.server.payment.config;

import com.sellect.server.order.Infrastructure.port.FakePayClient;
import com.sellect.server.order.Infrastructure.port.PayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class PaymentConfig {

    private final RestTemplate restTemplate;

    // 실 서비스용
//    @Bean
//    public PayClient payClient() {
//        return new KakaoPayClient(restTemplate);
//    }

    // 테스트용
    @Bean
    public PayClient payClient() {
        return new FakePayClient();
    }
}
