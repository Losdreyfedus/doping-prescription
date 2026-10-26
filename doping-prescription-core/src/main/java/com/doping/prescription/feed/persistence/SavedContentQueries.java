package com.doping.prescription.feed.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** A learner's saved videos (saved_content). Saving twice or removing an unsaved video changes nothing. */
@Repository
public class SavedContentQueries {

    private static final String SAVE = """
            insert into saved_content (user_id, content_id, created_at)
            values (:userId, :contentId, :savedAt)
            on conflict (user_id, content_id) do nothing
            """;

    private static final String REMOVE = """
            delete from saved_content where user_id = :userId and content_id = :contentId
            """;

    private static final String NEWEST_FIRST = """
            select content_id from saved_content where user_id = :userId order by created_at desc, content_id
            """;

    private static final String SAVED_AMONG = """
            select content_id from saved_content where user_id = :userId and content_id in (:contentIds)
            """;

    private final JdbcClient jdbc;

    SavedContentQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void save(UUID userId, Integer contentId, Instant savedAt) {
        jdbc.sql(SAVE)
                .param("userId", userId)
                .param("contentId", contentId)
                .param("savedAt", OffsetDateTime.ofInstant(savedAt, ZoneOffset.UTC))
                .update();
    }

    public void remove(UUID userId, Integer contentId) {
        jdbc.sql(REMOVE).param("userId", userId).param("contentId", contentId).update();
    }

    /** The learner's saved videos, most recently saved first. */
    public List<Integer> newestFirst(UUID userId) {
        return jdbc.sql(NEWEST_FIRST)
                .param("userId", userId)
                .query(Integer.class)
                .list();
    }

    /** Which of the videos the learner saved, in one query. */
    public Set<Integer> savedAmong(UUID userId, Collection<Integer> contentIds) {
        if (contentIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.sql(SAVED_AMONG)
                .param("userId", userId)
                .param("contentIds", contentIds)
                .query(Integer.class)
                .list());
    }
}
