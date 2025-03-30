package com.sellect.server.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.protocol:redis://}") // 기본값: 비암호화
    private String protocol;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = protocol + redisHost + ":" + redisPort;
        config.useSingleServer()
            .setAddress(address)
            .setConnectionMinimumIdleSize(5)
            .setConnectionPoolSize(64)
            .setTimeout(10000)
            .setConnectTimeout(10000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);
        return Redisson.create(config);
    }
}
