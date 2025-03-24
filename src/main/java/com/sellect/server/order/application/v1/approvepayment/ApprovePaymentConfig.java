package com.sellect.server.order.application.v1.approvepayment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApprovePaymentConfig {

    @Bean
    public ApprovePaymentStrategy approvePaymentStrategy(ApprovePaymentV1 v1) {
        return v1; // 데드락에 대한 회피 방식 (정렬) -> 기아현상 발생

    }
}
