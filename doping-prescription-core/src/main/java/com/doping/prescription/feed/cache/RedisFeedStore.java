package com.doping.prescription.feed.cache;

import com.doping.prescription.feed.application.FeedProperties;
import com.doping.prescription.feed.application.FeedStore;
import com.doping.prescription.feed.domain.ServedCard;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code feed:queue:{userId}} as a Redis list of card JSON and {@code feed:session:{sessionId}} as a set of content
 * ids (docs/mimari.md §5). Failures are logged and reported as "unavailable", never thrown.
 */
@Component
class RedisFeedStore implements FeedStore {

    private static final Logger log = LoggerFactory.getLogger(RedisFeedStore.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final FeedProperties properties;

    RedisFeedStore(StringRedisTemplate redis, JsonMapper json, FeedProperties properties) {
        this.redis = redis;
        this.json = json;
        this.properties = properties;
    }

    @Override
    public Optional<List<ServedCard>> queue(UUID userId) {
        try {
            List<String> values = redis.opsForList().range(queueKey(userId), 0, -1);
            return Optional.of(
                    values == null
                            ? List.of()
                            : values.stream()
                                    .map(value -> json.readValue(value, ServedCard.class))
                                    .toList());
        } catch (DataAccessException | JacksonException e) {
            log.warn("Feed queue read failed, computing the feed without it: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void replaceQueue(UUID userId, List<ServedCard> cards) {
        String key = queueKey(userId);
        try {
            redis.delete(key);
            if (!cards.isEmpty()) {
                redis.opsForList()
                        .rightPushAll(
                                key,
                                cards.stream().map(json::writeValueAsString).toList());
                redis.expire(key, properties.queueTtl());
            }
        } catch (DataAccessException | JacksonException e) {
            log.warn("Feed queue write failed: {}", e.getMessage());
        }
    }

    @Override
    public void dropQueue(UUID userId) {
        try {
            redis.delete(queueKey(userId));
        } catch (DataAccessException e) {
            log.warn("Feed queue eviction failed, it expires with its TTL: {}", e.getMessage());
        }
    }

    @Override
    public Optional<Set<Integer>> seen(UUID sessionId) {
        try {
            Set<String> members = redis.opsForSet().members(sessionKey(sessionId));
            return Optional.of(
                    members == null
                            ? Set.of()
                            : members.stream().map(Integer::valueOf).collect(Collectors.toSet()));
        } catch (DataAccessException e) {
            log.warn("Feed session read failed, reading it from impressions: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void markSeen(UUID sessionId, Collection<Integer> contentIds) {
        if (contentIds.isEmpty()) {
            return;
        }
        String key = sessionKey(sessionId);
        try {
            redis.opsForSet().add(key, contentIds.stream().map(String::valueOf).toArray(String[]::new));
            redis.expire(key, properties.sessionTtl());
        } catch (DataAccessException e) {
            // impressions remain the source of truth for the session
            log.warn("Feed session write failed: {}", e.getMessage());
        }
    }

    private static String queueKey(UUID userId) {
        return "feed:queue:" + userId;
    }

    private static String sessionKey(UUID sessionId) {
        return "feed:session:" + sessionId;
    }
}
