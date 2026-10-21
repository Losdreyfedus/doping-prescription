package com.doping.prescription.exam.application;

import com.doping.prescription.shared.api.UserDataEraser;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Erases one learner's exam data for the demo reset (docs/api.md, POST /api/admin/demo/reset). */
@Component
class ExamDataEraser implements UserDataEraser {

    /** Children before parents. */
    private static final List<String> STATEMENTS = List.of(
            "delete from exam_answer where attempt_id in (select id from exam_attempt where user_id = :userId)",
            "delete from exam_attempt where user_id = :userId");

    private final JdbcClient jdbc;

    ExamDataEraser(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String name() {
        return "exam";
    }

    @Override
    @Transactional
    public void erase(UUID userId) {
        STATEMENTS.forEach(sql -> jdbc.sql(sql).param("userId", userId).update());
    }
}
