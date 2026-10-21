package com.doping.prescription.quiz.cache;

import com.doping.prescription.quiz.application.QuizTokenRegistry;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** {@code quiz:token-used:{jti}} with SET NX and the token's lifetime as TTL. */
@Component
class RedisQuizTokenRegistry implements QuizTokenRegistry {

    private static final Logger log = LoggerFactory.getLogger(RedisQuizTokenRegistry.class);

    private final StringRedisTemplate redis;

    RedisQuizTokenRegistry(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean firstUse(UUID tokenId, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("quiz:token-used:" + tokenId, "1", ttl));
        } catch (DataAccessException e) {
            log.warn("Quiz token registry unavailable, relying on the database constraint: {}", e.getMessage());
            return true;
        }
    }
}
