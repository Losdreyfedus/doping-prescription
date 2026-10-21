package com.doping.prescription.points.application;

import com.doping.prescription.shared.api.UserDataEraser;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Erases one learner's points data for the demo reset (docs/api.md, POST /api/admin/demo/reset). */
@Component
class PointsDataEraser implements UserDataEraser {

    /** Children before parents. */
    private static final List<String> STATEMENTS = List.of(
            "delete from points_ledger where user_id = :userId", "delete from points_streak where user_id = :userId");

    private final JdbcClient jdbc;

    PointsDataEraser(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String name() {
        return "points";
    }

    @Override
    @Transactional
    public void erase(UUID userId) {
        STATEMENTS.forEach(sql -> jdbc.sql(sql).param("userId", userId).update());
    }
}
