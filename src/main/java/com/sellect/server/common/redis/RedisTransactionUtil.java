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
            public Void execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                commands.accept(operations);
                operations.exec();
                return null;
            }
        });
    }
}
