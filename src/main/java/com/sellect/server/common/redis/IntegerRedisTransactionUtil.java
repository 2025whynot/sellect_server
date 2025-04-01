package com.sellect.server.common.redis;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegerRedisTransactionUtil {
    private final RedisTemplate<String, Integer> integerRedisTemplate;

    @SuppressWarnings("unchecked")
    public <T> T transaction(Function<RedisOperations<String, Integer>, T> commands) {
        return integerRedisTemplate.execute(new SessionCallback<T>() {
            @Override
            public T execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                try {
                    T result = (T) commands.apply(operations);
                    operations.exec();
                    return result;
                } catch (Exception e) {
                    operations.discard();
                    throw e;
                }
            }
        });
    }

    // Getter 추가
    public RedisTemplate<String, Integer> getIntegerRedisTemplate() {
        return integerRedisTemplate;
    }
}