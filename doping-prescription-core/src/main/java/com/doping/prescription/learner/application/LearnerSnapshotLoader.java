package com.doping.prescription.learner.application;

import com.doping.prescription.learner.application.LearnerSnapshot.ExamSummary;
import com.doping.prescription.learner.application.LearnerSnapshot.TopicSnapshot;
import com.doping.prescription.learner.domain.SignalType;
import com.doping.prescription.learner.persistence.SignalQueries;
import com.doping.prescription.learner.persistence.TopicMastery;
import com.doping.prescription.learner.persistence.TopicMasteryRepository;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a learner's stored profile from PostgreSQL in one read-only transaction. Kept apart from the cache so the
 * Redis round trips of {@link LearnerProfileService} never run while a database connection is held.
 */
@Component
class LearnerSnapshotLoader {

    private final TopicMasteryRepository masteries;
    private final SignalQueries signalQueries;

    LearnerSnapshotLoader(TopicMasteryRepository masteries, SignalQueries signalQueries) {
        this.masteries = masteries;
        this.signalQueries = signalQueries;
    }

    @Transactional(readOnly = true)
    public LearnerSnapshot load(UUID userId) {
        Map<Integer, SignalType> lastSignals = signalQueries.lastMasterySignalPerTopic(userId);
        return new LearnerSnapshot(
                signalQueries.lastExamAttempt(userId).orElse(null),
                masteries.findByIdUserId(userId).stream()
                        .map(row -> snapshotOf(row, lastSignals.get(row.getTopicId())))
                        .toList());
    }

    private static TopicSnapshot snapshotOf(TopicMastery row, @Nullable SignalType lastSignal) {
        return new TopicSnapshot(
                row.getTopicId(),
                row.getMastery(),
                row.getConfidence(),
                row.getAttempts(),
                row.getLastSignalAt().orElse(null),
                lastSignal,
                row.getLastExamSummary()
                        .map(exam -> new ExamSummary(exam.attemptId(), exam.total(), exam.correct()))
                        .orElse(null));
    }
}
