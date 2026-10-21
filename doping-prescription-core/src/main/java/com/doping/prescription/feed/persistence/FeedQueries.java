package com.doping.prescription.feed.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Aggregate reads over impressions and interactions, one query each (no N+1). */
@Repository
public class FeedQueries {

    private static final String COMPLETED_SINCE = """
            select distinct impression.content_id
            from feed_interaction interaction
            join feed_impression impression on impression.id = interaction.impression_id
            where interaction.user_id = :userId and interaction.event = 'VIEW_COMPLETE'
              and interaction.client_ts >= :since
            """;

    private static final String SESSION_CONTENT = """
            select content_id from feed_impression where session_id = :sessionId
            """;

    private static final String SKIPS_PER_TOPIC = """
            select impression.target_topic_id, count(*) as skips
            from feed_interaction interaction
            join feed_impression impression on impression.id = interaction.impression_id
            where impression.session_id = :sessionId and interaction.event = 'SKIP'
              and impression.target_topic_id is not null
            group by impression.target_topic_id
            """;

    private static final String INSERT_INTERACTION = """
            insert into feed_interaction (impression_id, user_id, client_event_id, event, watch_ms, client_ts)
            values (:impressionId, :userId, :clientEventId, :event, :watchMs, :clientTs)
            on conflict (client_event_id) do nothing
            """;

    private static final String MAX_WATCH_MS = """
            select coalesce(max(watch_ms), 0) from feed_interaction where impression_id = :impressionId
            """;

    private final JdbcClient jdbc;

    FeedQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Videos the learner completed since the given instant; {@link Instant#EPOCH} means ever. */
    public Set<Integer> completedSince(UUID userId, Instant since) {
        return new HashSet<>(jdbc.sql(COMPLETED_SINCE)
                .param("userId", userId)
                .param("since", OffsetDateTime.ofInstant(since, ZoneOffset.UTC))
                .query(Integer.class)
                .list());
    }

    /** Videos already shown in a session; the source of truth when Redis is unavailable. */
    public Set<Integer> sessionContent(UUID sessionId) {
        return new HashSet<>(jdbc.sql(SESSION_CONTENT)
                .param("sessionId", sessionId)
                .query(Integer.class)
                .list());
    }

    public Map<Integer, Integer> skipsPerTopic(UUID sessionId) {
        Map<Integer, Integer> skips = new HashMap<>();
        jdbc.sql(SKIPS_PER_TOPIC)
                .param("sessionId", sessionId)
                .query((row, rowNumber) -> Map.entry(row.getInt("target_topic_id"), row.getInt("skips")))
                .list()
                .forEach(entry -> skips.put(entry.getKey(), entry.getValue()));
        return skips;
    }

    /**
     * Stores an interaction unless its client event id was seen before (AGENTS.md rule 6).
     *
     * @return true if it was stored
     */
    public boolean recordInteraction(NewInteraction interaction) {
        return jdbc.sql(INSERT_INTERACTION)
                        .param("impressionId", interaction.impressionId())
                        .param("userId", interaction.userId())
                        .param("clientEventId", interaction.clientEventId())
                        .param("event", interaction.event())
                        .param("watchMs", interaction.watchMs())
                        .param("clientTs", OffsetDateTime.ofInstant(interaction.clientTs(), ZoneOffset.UTC))
                        .update()
                == 1;
    }

    /** The furthest point the learner reported watching a card, in real video milliseconds. */
    public int maxWatchMs(UUID impressionId) {
        return jdbc.sql(MAX_WATCH_MS)
                .param("impressionId", impressionId)
                .query(Integer.class)
                .single();
    }

    /**
     * @param event   one of {@code InteractionType}'s names
     * @param watchMs real video milliseconds watched, if reported
     */
    public record NewInteraction(
            UUID impressionId, UUID userId, String clientEventId, String event, Integer watchMs, Instant clientTs) {}
}
