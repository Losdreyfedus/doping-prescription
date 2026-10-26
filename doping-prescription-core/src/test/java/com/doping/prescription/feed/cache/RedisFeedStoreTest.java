package com.doping.prescription.feed.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.doping.prescription.MovableClock;
import com.doping.prescription.feed.application.TestFeedProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

/** A Redis outage costs one timeout per retry window, not several per feed request (docs/mimari.md §5). */
class RedisFeedStoreTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final MovableClock clock = new MovableClock(NOW);
    private final RedisFeedStore store =
            new RedisFeedStore(redis, JsonMapper.builder().build(), TestFeedProperties.defaults(), clock);
    private final UUID learner = UUID.randomUUID();
    private final UUID session = UUID.randomUUID();

    @Test
    void afterAFailureRedisIsSkippedUntilTheRetryWindowEnds() {
        // Given: Redis is down
        when(redis.opsForList()).thenThrow(new RedisConnectionFailureException("down"));
        when(redis.opsForSet()).thenThrow(new RedisConnectionFailureException("down"));

        // When: the first read fails, and the rest of the request runs within the window
        assertThat(store.queue(learner)).isEmpty();
        assertThat(store.seen(learner, session)).isEmpty();
        store.markSeen(learner, session, List.of(33));
        store.replaceQueue(learner, List.of());

        // Then: only the first call reached Redis
        verify(redis, times(1)).opsForList();
        verify(redis, never()).opsForSet();
        verify(redis, never()).delete("feed:queue:" + learner);

        // When: the window is over
        clock.moveTo(NOW.plus(TestFeedProperties.STORE_RETRY_AFTER));

        // Then: Redis is tried again
        assertThat(store.seen(learner, session)).isEmpty();
        verify(redis, times(1)).opsForSet();
    }

    @Test
    void droppingAQueueIsNeverSkipped() {
        // Given: a failure started the retry window
        when(redis.opsForList()).thenThrow(new RedisConnectionFailureException("down"));
        store.queue(learner);

        // When: the learner's mastery changed
        store.dropQueue(learner);

        // Then: the stale queue is still deleted
        verify(redis).delete("feed:queue:" + learner);
    }
}
