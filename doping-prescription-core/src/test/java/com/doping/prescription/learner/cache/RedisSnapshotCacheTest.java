package com.doping.prescription.learner.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.doping.prescription.MovableClock;
import com.doping.prescription.learner.application.LearnerProperties;
import com.doping.prescription.learner.application.LearnerSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

/** Profile reads skip an unavailable Redis for a while; the write path never does (docs/mimari.md §5). */
class RedisSnapshotCacheTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration RETRY_AFTER = Duration.ofSeconds(10);
    private static final LearnerSnapshot EMPTY = new LearnerSnapshot(null, List.of());

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);

    private final MovableClock clock = new MovableClock(NOW);
    private final RedisSnapshotCache cache = new RedisSnapshotCache(
            redis, JsonMapper.builder().build(), new LearnerProperties(null, TTL, RETRY_AFTER), clock);
    private final UUID learner = UUID.randomUUID();

    @BeforeEach
    void redisIsDown() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
    }

    @Test
    void afterAFailedReadTheCacheIsSkippedUntilTheRetryWindowEnds() {
        // When: the first read fails, then more reads and a fill within the window
        assertThat(cache.get(learner)).isEmpty();
        assertThat(cache.get(learner)).isEmpty();
        cache.putIfAbsent(learner, EMPTY);

        // Then: Redis saw only the first read
        verify(redis, times(1)).opsForValue();

        // When: the window is over
        clock.moveTo(NOW.plus(RETRY_AFTER));
        assertThat(cache.get(learner)).isEmpty();

        // Then
        verify(values, times(2)).get("mastery:" + learner);
    }

    @Test
    void theWritePathStillReachesRedisDuringTheWindow() {
        // Given: a failed read started the window
        cache.get(learner);

        // When: a committed write refreshes the cache
        cache.replace(learner, EMPTY);

        // Then: the stale entry is overwritten, not left behind
        verify(values).set(eq("mastery:" + learner), anyString(), eq(TTL));
    }
}
