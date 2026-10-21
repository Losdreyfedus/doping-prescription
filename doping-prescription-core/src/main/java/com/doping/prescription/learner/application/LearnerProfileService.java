package com.doping.prescription.learner.application;

import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.learner.api.LearnerProfileQuery;
import com.doping.prescription.learner.application.LearnerSnapshot.ExamSummary;
import com.doping.prescription.learner.application.LearnerSnapshot.TopicSnapshot;
import com.doping.prescription.learner.domain.MasteryBand;
import com.doping.prescription.learner.domain.MasteryCalculator;
import com.doping.prescription.learner.domain.SignalType;
import com.doping.prescription.learner.domain.Trend;
import com.doping.prescription.learner.persistence.SignalQueries;
import com.doping.prescription.learner.persistence.TopicMastery;
import com.doping.prescription.learner.persistence.TopicMasteryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a learner's profile through the cache and derives decay, bands and trends at read time. */
@Service
public class LearnerProfileService implements LearnerProfileQuery {

    private final TopicMasteryRepository masteries;
    private final SignalQueries signalQueries;
    private final SnapshotCache cache;
    private final TopicCatalog topics;
    private final MasteryCalculator calculator;
    private final LearnerProperties properties;
    private final Clock clock;

    LearnerProfileService(
            TopicMasteryRepository masteries,
            SignalQueries signalQueries,
            SnapshotCache cache,
            TopicCatalog topics,
            MasteryCalculator calculator,
            LearnerProperties properties,
            Clock clock) {
        this.masteries = masteries;
        this.signalQueries = signalQueries;
        this.cache = cache;
        this.topics = topics;
        this.calculator = calculator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public LearnerProfile profile(UUID userId) {
        LearnerSnapshot snapshot = cache.get(userId).orElseGet(() -> {
            LearnerSnapshot loaded = load(userId);
            cache.putIfAbsent(userId, loaded);
            return loaded;
        });
        Map<Integer, TopicSnapshot> byTopic =
                snapshot.topics().stream().collect(Collectors.toMap(TopicSnapshot::topicId, Function.identity()));
        Instant now = clock.instant();
        return new LearnerProfile(
                userId,
                Optional.ofNullable(snapshot.lastExamAttemptId()),
                topics.allTopics().stream()
                        .map(topic -> topicProfile(topic.topicId(), byTopic.get(topic.topicId()), now))
                        .toList());
    }

    /** Reloads the snapshot after a committed write and overwrites the cached one. */
    @Transactional(readOnly = true)
    public void refreshCache(UUID userId) {
        cache.replace(userId, load(userId));
    }

    private TopicProfile topicProfile(Integer topicId, @Nullable TopicSnapshot topic, Instant now) {
        Instant lastSignalAt = topic == null ? null : topic.lastSignalAt();
        if (topic == null || lastSignalAt == null) {
            return new TopicProfile(
                    topicId,
                    properties.mastery().initial(),
                    0,
                    MasteryBand.UNKNOWN.name(),
                    Trend.FLAT.name(),
                    0,
                    Optional.empty(),
                    Optional.empty());
        }
        double effective = calculator.effectiveMastery(topic.mastery(), lastSignalAt, now);
        MasteryBand band = MasteryBand.of(
                effective, topic.attempts(), properties.mastery().band().thresholds());
        return new TopicProfile(
                topicId,
                effective,
                topic.confidence(),
                band.name(),
                Trend.fromLastSignal(Optional.ofNullable(topic.lastSignal())).name(),
                topic.attempts(),
                Optional.of(lastSignalAt),
                Optional.ofNullable(topic.lastExam())
                        .map(exam -> new LastExam(exam.attemptId(), exam.total(), exam.correct())));
    }

    private LearnerSnapshot load(UUID userId) {
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
