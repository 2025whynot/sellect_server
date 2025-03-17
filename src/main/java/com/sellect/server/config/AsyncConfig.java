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

    @Bean(name = "paymentTaskExecutor")
    public ThreadPoolTaskExecutor payTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 최소 스레드 개수
        executor.setMaxPoolSize(10); // 최대 스레드 개수
        executor.setQueueCapacity(50); // 대기 큐 크기
        executor.setThreadNamePrefix("paymentTaskExecutor AsyncThread - "); // 스레드 이름 지정
        executor.initialize();
        return executor;
    }
}
