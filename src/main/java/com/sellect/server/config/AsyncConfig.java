package com.sellect.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync // 비동기 처리 활성화
public class AsyncConfig {
    @Bean(name = "asyncTaskExecutor")
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 최소 스레드 개수
        executor.setMaxPoolSize(10); // 최대 스레드 개수
        executor.setQueueCapacity(100); // 대기 큐 크기
        executor.setThreadNamePrefix("SearchLog AsyncThread - "); // 스레드 이름 지정
        executor.initialize();
        return executor;
    }

//    @Bean(name = "preparePaymentExecutor")
//    public ThreadPoolTaskExecutor preparePaymentExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(5);
//        executor.setMaxPoolSize(10);
//        executor.setQueueCapacity(50);
//        executor.setThreadNamePrefix("preparePaymentExecutor AsyncThread - ");
//        executor.initialize();
//        return executor;
//    }

    @Bean(name = "approvePaymentExecutor")
    public ThreadPoolTaskExecutor approvePaymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("approvePaymentExecutor AsyncThread - ");
        executor.initialize();
        return executor;
    }


    // V5, V6 레디스 재고 차감 후 이벤트 발행 처리 리스너
    @Bean(name = "approvePaymentRedisExecutor")
    public ThreadPoolTaskExecutor approvePaymentRedisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("approvePaymentRedisExecutor AsyncThread - ");
        executor.initialize();
        return executor;
    }

    // preparePayment 보상 트랜잭션 이벤트 큐
    @Bean(name = "preparePaymentCompensationExecutor")
    public ThreadPoolTaskExecutor preparePaymentCompensationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("preparePaymentCompensationExecutor AsyncThread - ");
        executor.initialize();
        return executor;
    }

    // approvePayment 보상 트랜잭션 이벤트 큐
    @Bean(name = "approvePaymentCompensationExecutor")
    public ThreadPoolTaskExecutor approvePaymentCompensationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("approvePaymentCompensationExecutor AsyncThread - ");
        executor.initialize();
        return executor;
    }
}
