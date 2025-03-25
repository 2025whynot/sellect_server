package com.sellect.server.common.redis;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisTransactionUtil {

    private final RedisTemplate<String, String> redisTemplate;

    public void transaction(Consumer<RedisOperations<String, String>> commands) {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                try {
                    commands.accept(operations);
                    operations.exec();
                    return null;
                } catch (Exception e) {
                    operations.discard();
                    throw e;
                }
            }
        });
    }
}
