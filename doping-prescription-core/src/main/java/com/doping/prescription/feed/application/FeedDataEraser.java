package com.doping.prescription.feed.application;

import com.doping.prescription.shared.api.UserDataEraser;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Erases one learner's feed data for the demo reset (docs/api.md, POST /api/admin/demo/reset). */
@Component
class FeedDataEraser implements UserDataEraser {

    /** Children before parents. */
    private static final List<String> STATEMENTS = List.of(
            "delete from feed_interaction where user_id = :userId",
            "delete from feed_impression where user_id = :userId");

    private final JdbcClient jdbc;
    private final FeedStore store;

    FeedDataEraser(JdbcClient jdbc, FeedStore store) {
        this.jdbc = jdbc;
        this.store = store;
    }

    @Override
    public String name() {
        return "feed";
    }

    @Override
    @Transactional
    public void erase(UUID userId) {
        STATEMENTS.forEach(sql -> jdbc.sql(sql).param("userId", userId).update());
        store.dropQueue(userId);
    }
}
