package com.doping.prescription.learner.application;

import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.exam.api.ExamResultPublished.AnswerOutcome;
import com.doping.prescription.learner.api.MasteryChanged;
import com.doping.prescription.learner.api.MasteryChanged.TopicChange;
import com.doping.prescription.learner.domain.MasteryCalculator;
import com.doping.prescription.learner.domain.MasteryRounding;
import com.doping.prescription.learner.domain.MasteryState;
import com.doping.prescription.learner.domain.SignalType;
import com.doping.prescription.learner.persistence.LastExamSummary;
import com.doping.prescription.learner.persistence.LearnerSignal;
import com.doping.prescription.learner.persistence.LearnerSignalRepository;
import com.doping.prescription.learner.persistence.TopicMastery;
import com.doping.prescription.learner.persistence.TopicMasteryId;
import com.doping.prescription.learner.persistence.TopicMasteryRepository;
import com.doping.prescription.shared.idempotency.ProcessedEvents;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns learning evidence into topic mastery (docs/algoritma.md §1) and announces significant changes. */
@Service
public class MasteryService {

    static final String CONSUMER = "learner";
    static final String EXAM_TRIGGER = "EXAM_RESULT";

    private final TopicMasteryRepository masteries;
    private final LearnerSignalRepository signals;
    private final ProcessedEvents processedEvents;
    private final MasteryCalculator calculator;
    private final LearnerProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    MasteryService(
            TopicMasteryRepository masteries,
            LearnerSignalRepository signals,
            ProcessedEvents processedEvents,
            MasteryCalculator calculator,
            LearnerProperties properties,
            ApplicationEventPublisher events,
            Clock clock) {
        this.masteries = masteries;
        this.signals = signals;
        this.processedEvents = processedEvents;
        this.calculator = calculator;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Applies an exam result question by question in exam order. A redelivered event, or a question already recorded,
     * changes nothing (AGENTS.md §5.6). Callers refresh the cached profile after this method returns, once the new
     * values are committed.
     */
    @Transactional
    public void applyExamResult(ExamResultPublished result) {
        if (!processedEvents.firstDelivery(CONSUMER, result.eventId())) {
            return;
        }
        UUID userId = result.userId();
        Map<Integer, TopicMastery> rows = masteries.findByIdUserId(userId).stream()
                .collect(Collectors.toMap(TopicMastery::getTopicId, Function.identity()));
        Map<Integer, Double> before = new LinkedHashMap<>();
        Map<Integer, ExamTally> tallies = new LinkedHashMap<>();
        Instant now = clock.instant();

        for (AnswerOutcome answer : result.answers()) {
            String idempotencyKey = "exam:" + result.attemptId() + ":" + answer.questionId();
            if (signals.existsByIdempotencyKey(idempotencyKey)) {
                continue;
            }
            SignalType type = signalType(answer.result());
            signals.save(new LearnerSignal(
                    userId,
                    answer.topicId(),
                    type,
                    result.attemptId().toString(),
                    result.occurredAt(),
                    idempotencyKey));
            TopicMastery row = rows.computeIfAbsent(answer.topicId(), topicId -> newRow(userId, topicId, now));
            before.putIfAbsent(answer.topicId(), row.getMastery());
            boolean correct = MasteryCalculator.isCorrect(type);
            MasteryState next = calculator.apply(stateOf(row), type);
            row.recordSignal(next.mastery(), next.confidence(), correct, result.occurredAt());
            tallies.computeIfAbsent(answer.topicId(), topicId -> new ExamTally())
                    .add(correct);
        }
        tallies.forEach((topicId, tally) -> rows.get(topicId)
                .recordExamSummary(new LastExamSummary(result.attemptId(), tally.total, tally.correct), now));
        masteries.saveAll(rows.values());

        publishSignificantChanges(userId, EXAM_TRIGGER, before, rows, now);
    }

    /**
     * A feed card of the topic was watched or skipped (docs/mimari.md §4). Only topics the learner already has a
     * mastery row for are touched; serving a video is no learning evidence.
     */
    @Transactional
    public void recordServed(UUID eventId, UUID userId, Integer topicId, Instant servedAt) {
        if (processedEvents.firstDelivery(CONSUMER, eventId)) {
            masteries.findById(new TopicMasteryId(userId, topicId)).ifPresent(row -> row.recordServed(servedAt));
        }
    }

    private void publishSignificantChanges(
            UUID userId, String trigger, Map<Integer, Double> before, Map<Integer, TopicMastery> rows, Instant now) {
        double threshold = properties.mastery().changeEventThreshold();
        List<TopicChange> changes = new ArrayList<>();
        before.forEach((topicId, previous) -> {
            TopicMastery row = rows.get(topicId);
            if (Math.abs(row.getMastery() - previous) >= threshold) {
                changes.add(new TopicChange(
                        topicId,
                        MasteryRounding.forApi(previous).doubleValue(),
                        MasteryRounding.forApi(row.getMastery()).doubleValue(),
                        MasteryRounding.forApi(row.getConfidence()).doubleValue()));
            }
        });
        if (!changes.isEmpty()) {
            events.publishEvent(new MasteryChanged(
                    UUID.randomUUID(), MasteryChanged.SCHEMA_VERSION, now, userId, trigger, changes));
        }
    }

    private TopicMastery newRow(UUID userId, Integer topicId, Instant now) {
        return new TopicMastery(
                new TopicMasteryId(userId, topicId), properties.mastery().initial(), now);
    }

    private static MasteryState stateOf(TopicMastery row) {
        return new MasteryState(row.getMastery(), row.getConfidence(), row.getAttempts(), row.getCorrect());
    }

    private static SignalType signalType(String result) {
        return switch (result) {
            case "CORRECT" -> SignalType.EXAM_CORRECT;
            case "WRONG" -> SignalType.EXAM_WRONG;
            case "BLANK" -> SignalType.EXAM_BLANK;
            default -> throw new IllegalArgumentException("Unknown exam answer result: " + result);
        };
    }

    /** Questions and correct answers of one topic within one exam. */
    private static final class ExamTally {

        private int total;
        private int correct;

        void add(boolean wasCorrect) {
            total++;
            if (wasCorrect) {
                correct++;
            }
        }
    }
}
