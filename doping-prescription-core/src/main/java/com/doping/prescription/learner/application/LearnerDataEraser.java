package com.doping.prescription.learner.application;

import com.doping.prescription.shared.api.UserDataEraser;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Erases one learner's learner data for the demo reset (docs/api.md, POST /api/admin/demo/reset). */
@Component
class LearnerDataEraser implements UserDataEraser {

    /** Children before parents. */
    private static final List<String> STATEMENTS = List.of(
            "delete from learner_signal where user_id = :userId",
            "delete from learner_topic_mastery where user_id = :userId");

    private final JdbcClient jdbc;
    private final SnapshotCache cache;

    LearnerDataEraser(JdbcClient jdbc, SnapshotCache cache) {
        this.jdbc = jdbc;
        this.cache = cache;
    }

    @Override
    public String name() {
        return "learner";
    }

    @Override
    @Transactional
    public void erase(UUID userId) {
        STATEMENTS.forEach(sql -> jdbc.sql(sql).param("userId", userId).update());
        cache.evict(userId);
    }
}
