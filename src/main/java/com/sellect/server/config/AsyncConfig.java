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

    @Bean(name = "preparePaymentExecutor")
    public ThreadPoolTaskExecutor preparePaymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 최소 스레드 개수
        executor.setMaxPoolSize(10); // 최대 스레드 개수
        executor.setQueueCapacity(50); // 대기 큐 크기
        executor.setThreadNamePrefix("preparePaymentExecutor AsyncThread - "); // 스레드 이름 지정
        executor.initialize();
        return executor;
    }

    @Bean(name = "approvePaymentExecutor")
    public ThreadPoolTaskExecutor approvePaymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 최소 스레드 개수
        executor.setMaxPoolSize(10); // 최대 스레드 개수
        executor.setQueueCapacity(50); // 대기 큐 크기
        executor.setThreadNamePrefix("approvePaymentExecutor AsyncThread - "); // 스레드 이름 지정
        executor.initialize();
        return executor;
    }

    // preparePayment 보상 트랜잭션 이벤트 큐
    @Bean(name = "preparePaymentCompensationExecutor")
    public ThreadPoolTaskExecutor preparePaymentCompensationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 최소 스레드 개수
        executor.setMaxPoolSize(10); // 최대 스레드 개수
        executor.setQueueCapacity(50); // 대기 큐 크기
        executor.setThreadNamePrefix("preparePaymentCompensationExecutor AsyncThread - "); // 스레드 이름 지정
        executor.initialize();
        return executor;
    }
}
