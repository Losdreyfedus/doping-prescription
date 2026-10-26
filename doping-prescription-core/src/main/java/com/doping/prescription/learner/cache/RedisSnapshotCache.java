package com.doping.prescription.learner.cache;

import com.doping.prescription.learner.application.LearnerProperties;
import com.doping.prescription.learner.application.LearnerSnapshot;
import com.doping.prescription.learner.application.SnapshotCache;
import com.doping.prescription.shared.time.Cooldown;
import java.time.Clock;
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
 *
 * <p>After a Redis failure the read path ({@link #get} and {@link #putIfAbsent}) skips Redis for
 * {@code prescription.learner.cache-retry-after}, so an outage costs one timeout per window instead of one per read.
 * The write path ({@link #replace}, {@link #evict}) never skips: a stale entry it failed to overwrite could otherwise
 * outlive the outage.
 */
@Component
class RedisSnapshotCache implements SnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(RedisSnapshotCache.class);
    private static final String KEY_PREFIX = "mastery:";

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final LearnerProperties properties;
    private final Cooldown unavailable;

    RedisSnapshotCache(StringRedisTemplate redis, JsonMapper json, LearnerProperties properties, Clock clock) {
        this.redis = redis;
        this.json = json;
        this.properties = properties;
        this.unavailable = new Cooldown(clock, properties.cacheRetryAfter());
    }

    @Override
    public Optional<LearnerSnapshot> get(UUID userId) {
        if (unavailable.active()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(redis.opsForValue().get(key(userId)))
                    .map(value -> json.readValue(value, LearnerSnapshot.class));
        } catch (DataAccessException e) {
            failed("Profile cache read failed, falling back to the database", e);
            return Optional.empty();
        } catch (JacksonException e) {
            log.warn("Profile cache entry unreadable, falling back to the database: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void putIfAbsent(UUID userId, LearnerSnapshot snapshot) {
        if (unavailable.active()) {
            return;
        }
        try {
            redis.opsForValue().setIfAbsent(key(userId), json.writeValueAsString(snapshot), properties.cacheTtl());
        } catch (DataAccessException e) {
            failed("Profile cache write failed", e);
        } catch (JacksonException e) {
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

    private void failed(String message, DataAccessException e) {
        unavailable.start();
        log.warn("{}; skipping the cache for {}: {}", message, properties.cacheRetryAfter(), e.getMessage());
    }

    private static String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
