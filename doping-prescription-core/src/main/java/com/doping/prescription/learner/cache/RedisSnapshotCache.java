package com.doping.prescription.learner.cache;

import com.doping.prescription.learner.application.LearnerProperties;
import com.doping.prescription.learner.application.LearnerSnapshot;
import com.doping.prescription.learner.application.SnapshotCache;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps each learner's snapshot as one JSON string under {@code mastery:{userId}} (docs/mimari.md §5). If Redis is
 * unreachable the profile is read from PostgreSQL, so errors are logged and swallowed.
 */
@Component
class RedisSnapshotCache implements SnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(RedisSnapshotCache.class);
    private static final String KEY_PREFIX = "mastery:";

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final LearnerProperties properties;

    RedisSnapshotCache(StringRedisTemplate redis, JsonMapper json, LearnerProperties properties) {
        this.redis = redis;
        this.json = json;
        this.properties = properties;
    }

    @Override
    public Optional<LearnerSnapshot> get(UUID userId) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key(userId)))
                    .map(value -> json.readValue(value, LearnerSnapshot.class));
        } catch (DataAccessException | JacksonException e) {
            log.warn("Profile cache read failed, falling back to the database: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void putIfAbsent(UUID userId, LearnerSnapshot snapshot) {
        try {
            redis.opsForValue().setIfAbsent(key(userId), json.writeValueAsString(snapshot), properties.cacheTtl());
        } catch (DataAccessException | JacksonException e) {
            log.warn("Profile cache write failed: {}", e.getMessage());
        }
    }

    @Override
    public void replace(UUID userId, LearnerSnapshot snapshot) {
        try {
            redis.opsForValue().set(key(userId), json.writeValueAsString(snapshot), properties.cacheTtl());
        } catch (DataAccessException | JacksonException e) {
            // a stale entry would outlive the write; dropping it is the next best thing
            log.warn("Profile cache refresh failed: {}", e.getMessage());
            evictQuietly(userId);
        }
    }

    @Override
    public void evict(UUID userId) {
        evictQuietly(userId);
    }

    private void evictQuietly(UUID userId) {
        try {
            redis.delete(key(userId));
        } catch (DataAccessException e) {
            log.warn("Profile cache eviction failed, the entry expires with its TTL: {}", e.getMessage());
        }
    }

    private static String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
