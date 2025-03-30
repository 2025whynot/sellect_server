package com.sellect.server.order.application.v1.approvepayment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApprovePaymentConfig {

//    @Bean
//    public ApprovePaymentStrategy approvePaymentStrategy(ApprovePaymentV0 v0) {
//        return v0; // 기존 코드: 데드락에 대한 아무런 조치가 없음
//    }

    @Bean
    public ApprovePaymentStrategy approvePaymentStrategy(ApprovePaymentV1 v1) {
        return v1; // 방법 1. 데드락에 대한 회피 방식 (정렬) -> 기아현상 발생
    }
//
//    @Bean
//    public ApprovePaymentStrategy approvePaymentStrategy(ApprovePaymentV2 v2) {
//        return v2; // 방법 2. 격리 수준을 SERIALIZABLE로 변경 (어느 정도 성능상 안 좋은지 직접 확인해보고 싶음)
//    }
//
//    @Bean
//    public ApprovePaymentStrategy approvePaymentStrategy(ApprovePaymentV3 v3) {
//        return v3; // 방법 3. 데드락 발생 시 재시도 로직 추가
//    }
//
//    @Bean
//    public ApprovePaymentStrategy approvePaymentStrategy(ApprovePaymentV4 v4) {
//        return v4; // 방법 4. 레디스를 분산락으로 제어, 재고 차감은 MySQL 그대로 이용 (다만, 비관적 락을 레디스 분산락으로 변경)
//    }
//
//    @Bean
//    public ApprovePaymentStrategy approvePaymentStrategy(ApprovePaymentV5 v5) {
//        return v5; // 방법 5. 레디스를 PID 락, 멀티락으로 재고 차감은 Redis 사용 (Multi 락)
//    }
//
//    @Bean
//    public ApprovePaymentStrategy approvePaymentStrategy(ApprovePaymentV6 v6) {
//        return v6; // 방법 6. 분산락으로 동시성 제어, 레디스로 재고 차감
//    }
}
