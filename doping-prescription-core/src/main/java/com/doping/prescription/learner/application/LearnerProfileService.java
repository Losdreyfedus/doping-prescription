package com.doping.prescription.learner.application;

import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.learner.api.LearnerProfileQuery;
import com.doping.prescription.learner.application.LearnerSnapshot.TopicSnapshot;
import com.doping.prescription.learner.domain.MasteryBand;
import com.doping.prescription.learner.domain.MasteryCalculator;
import com.doping.prescription.learner.domain.SignalType;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Reads a learner's profile through the cache and derives decay, bands and trends at read time. The cache is used
 * outside any transaction, so a slow Redis never holds a database connection; only a miss reads PostgreSQL.
 */
@Service
public class LearnerProfileService implements LearnerProfileQuery {

    private final LearnerSnapshotLoader loader;
    private final SnapshotCache cache;
    private final TopicCatalog topics;
    private final MasteryCalculator calculator;
    private final LearnerProperties properties;
    private final Clock clock;

    LearnerProfileService(
            LearnerSnapshotLoader loader,
            SnapshotCache cache,
            TopicCatalog topics,
            MasteryCalculator calculator,
            LearnerProperties properties,
            Clock clock) {
        this.loader = loader;
        this.cache = cache;
        this.topics = topics;
        this.calculator = calculator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public LearnerProfile profile(UUID userId) {
        LearnerSnapshot snapshot = cache.get(userId).orElseGet(() -> {
            LearnerSnapshot loaded = loader.load(userId);
            cache.putIfAbsent(userId, loaded);
            return loaded;
        });
        Map<Integer, TopicSnapshot> byTopic =
                snapshot.topics().stream().collect(Collectors.toMap(TopicSnapshot::topicId, Function.identity()));
        Instant now = clock.instant();
        return new LearnerProfile(
                userId,
                snapshot.lastExamAttemptId(),
                topics.allTopics().stream()
                        .map(topic -> topicProfile(topic.topicId(), byTopic.get(topic.topicId()), now))
                        .toList());
    }

    /** Reloads the snapshot after a committed write and overwrites the cached one. */
    public void refreshCache(UUID userId) {
        cache.replace(userId, loader.load(userId));
    }

    private TopicProfile topicProfile(Integer topicId, @Nullable TopicSnapshot topic, Instant now) {
        Instant lastSignalAt = topic == null ? null : topic.lastSignalAt();
        if (topic == null || lastSignalAt == null) {
            return new TopicProfile(
                    topicId, properties.mastery().initial(), 0, Band.UNKNOWN, Trend.FLAT, 0, null, null);
        }
        double effective = calculator.effectiveMastery(topic.mastery(), lastSignalAt, now);
        MasteryBand band = MasteryBand.of(
                effective, topic.attempts(), properties.mastery().band().thresholds());
        LearnerSnapshot.ExamSummary exam = topic.lastExam();
        return new TopicProfile(
                topicId,
                effective,
                topic.confidence(),
                toApi(band),
                trend(topic.lastSignal()),
                topic.attempts(),
                lastSignalAt,
                exam == null ? null : new LastExam(exam.attemptId(), exam.total(), exam.correct()));
    }

    /**
     * The trend of the latest signal as the api enum. {@code Trend} alone is the inherited api type here, so the domain
     * type is spelled out.
     */
    private static Trend trend(@Nullable SignalType lastSignal) {
        return switch (com.doping.prescription.learner.domain.Trend.fromLastSignal(Optional.ofNullable(lastSignal))) {
            case UP -> Trend.UP;
            case DOWN -> Trend.DOWN;
            case FLAT -> Trend.FLAT;
        };
    }

    /** The domain band as the api enum; a new domain band does not compile until it is mapped here. */
    static Band toApi(MasteryBand band) {
        return switch (band) {
            case UNKNOWN -> Band.UNKNOWN;
            case WEAK -> Band.WEAK;
            case MEDIUM -> Band.MEDIUM;
            case STRONG -> Band.STRONG;
        };
    }
}
