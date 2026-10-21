package com.doping.prescription.flow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Wipes one student's activity between flow tests that share a Spring context and its containers. Statements run in
 * dependency order; Redis keys are removed too, so no cached profile or queue survives.
 */
final class StudentReset {

    private static final List<String> STATEMENTS = List.of(
            "delete from feed_interaction where user_id = :userId",
            "delete from feed_impression where user_id = :userId",
            "delete from exam_answer where attempt_id in (select id from exam_attempt where user_id = :userId)",
            "delete from exam_attempt where user_id = :userId",
            "delete from learner_signal where user_id = :userId",
            "delete from learner_topic_mastery where user_id = :userId");

    private StudentReset() {}

    static UUID reset(JdbcClient jdbc, StringRedisTemplate redis, String slug) {
        UUID userId = jdbc.sql("select id from users where slug = :slug")
                .param("slug", slug)
                .query(UUID.class)
                .single();
        STATEMENTS.forEach(sql -> jdbc.sql(sql).param("userId", userId).update());
        redis.delete(List.of("mastery:" + userId, "feed:queue:" + userId));
        return userId;
    }
}
