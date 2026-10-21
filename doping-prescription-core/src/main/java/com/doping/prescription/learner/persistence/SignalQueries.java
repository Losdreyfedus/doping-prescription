package com.doping.prescription.learner.persistence;

import com.doping.prescription.learner.domain.SignalType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Aggregate reads over the signal log, one query each regardless of the number of topics (no N+1). */
@Repository
public class SignalQueries {

    private static final String LAST_MASTERY_SIGNAL_PER_TOPIC = """
            select distinct on (topic_id) topic_id, signal_type
            from learner_signal
            where user_id = :userId and signal_type not in ('VIDEO_COMPLETED', 'VIDEO_SKIPPED')
            order by topic_id, occurred_at desc, id desc
            """;

    private static final String LAST_EXAM_ATTEMPT = """
            select source_ref from learner_signal
            where user_id = :userId and signal_type in ('EXAM_CORRECT', 'EXAM_WRONG', 'EXAM_BLANK')
            order by occurred_at desc, id desc
            limit 1
            """;

    private final JdbcClient jdbc;

    SignalQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** The latest mastery-affecting signal per topic, for the trend arrow. */
    public Map<Integer, SignalType> lastMasterySignalPerTopic(UUID userId) {
        Map<Integer, SignalType> signals = new HashMap<>();
        jdbc.sql(LAST_MASTERY_SIGNAL_PER_TOPIC)
                .param("userId", userId)
                .query((row, rowNumber) ->
                        Map.entry(row.getInt("topic_id"), SignalType.valueOf(row.getString("signal_type"))))
                .list()
                .forEach(entry -> signals.put(entry.getKey(), entry.getValue()));
        return signals;
    }

    /** The exam attempt of the learner's latest exam signal. */
    public Optional<UUID> lastExamAttempt(UUID userId) {
        return jdbc.sql(LAST_EXAM_ATTEMPT)
                .param("userId", userId)
                .query(String.class)
                .optional()
                .map(UUID::fromString);
    }
}
