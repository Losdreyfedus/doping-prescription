package com.doping.prescription.learner.application;

import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.exam.api.ExamResultPublished.AnswerOutcome;
import com.doping.prescription.learner.api.LearnerSignalRecorder.MasteryChange;
import com.doping.prescription.learner.api.MasteryChanged;
import com.doping.prescription.learner.api.MasteryChanged.TopicChange;
import com.doping.prescription.learner.domain.MasteryCalculator;
import com.doping.prescription.learner.domain.MasteryChangePolicy;
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
import java.util.Set;
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
    /** The feed interactions that serve a video to the learner (feed.interaction-recorded). */
    private static final Set<String> SERVED = Set.of("VIEW_COMPLETE", "SKIP");

    private final TopicMasteryRepository masteries;
    private final LearnerSignalRepository signals;
    private final ProcessedEvents processedEvents;
    private final MasteryCalculator calculator;
    private final MasteryChangePolicy changePolicy;
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
        this.changePolicy = new MasteryChangePolicy(properties.mastery().changeEventThreshold());
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
        Set<String> recorded = signals.findRecordedKeys(
                result.answers().stream().map(answer -> examKey(result, answer)).toList());

        for (AnswerOutcome answer : result.answers()) {
            String idempotencyKey = examKey(result, answer);
            if (recorded.contains(idempotencyKey)) {
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
     * Applies one micro-question answer (docs/algoritma.md §1, QUIZ weight). The before value is the decayed mastery
     * the learner saw on Gelişimim; a repeated {@code refId} returns the current value as both before and after.
     */
    @Transactional
    public MasteryChange applyQuizAnswer(
            UUID userId, Integer topicId, boolean correct, String refId, Instant answeredAt) {
        TopicMasteryId id = new TopicMasteryId(userId, topicId);
        TopicMastery row = masteries.findForUpdate(id).orElseGet(() -> newRow(userId, topicId, answeredAt));
        // TODO(spec): docs/algoritma.md §1 applies the step to "m: mevcut mastery" and says decay is never written to
        // the table, so the update starts from the stored (undecayed) mastery, like an exam signal. "before" is the
        // decayed value the learner saw on Gelişimim, so after days of inactivity "after - before" also contains the
        // decay being undone. Same-day answers (the demo) are unaffected: whole-day decay is 0 within a day.
        double before = row.getLastSignalAt()
                .map(last -> calculator.effectiveMastery(row.getMastery(), last, answeredAt))
                .orElse(row.getMastery());
        String idempotencyKey = "quiz:" + refId;
        if (signals.existsByIdempotencyKey(idempotencyKey)) {
            return new MasteryChange(topicId, before, before);
        }
        SignalType type = correct ? SignalType.QUIZ_CORRECT : SignalType.QUIZ_WRONG;
        signals.save(new LearnerSignal(userId, topicId, type, refId, answeredAt, idempotencyKey));
        MasteryState next = calculator.apply(stateOf(row), type);
        row.recordSignal(next.mastery(), next.confidence(), correct, answeredAt);
        masteries.save(row);
        publishSignificantChanges(userId, type.name(), Map.of(topicId, before), Map.of(topicId, row), clock.instant());
        return new MasteryChange(topicId, before, row.getMastery());
    }

    /**
     * A feed card of the topic was watched or skipped (docs/mimari.md §4); other interactions (start, like, quiz shown)
     * change nothing. Only topics the learner already has a mastery row for are touched; serving a video is no
     * learning evidence.
     */
    @Transactional
    public void recordServed(UUID eventId, UUID userId, Integer topicId, String interaction, Instant servedAt) {
        if (SERVED.contains(interaction) && processedEvents.firstDelivery(CONSUMER, eventId)) {
            masteries.findById(new TopicMasteryId(userId, topicId)).ifPresent(row -> row.recordServed(servedAt));
        }
    }

    private void publishSignificantChanges(
            UUID userId, String trigger, Map<Integer, Double> before, Map<Integer, TopicMastery> rows, Instant now) {
        List<TopicChange> changes = new ArrayList<>();
        before.forEach((topicId, previous) -> {
            TopicMastery row = rows.get(topicId);
            if (changePolicy.isSignificant(previous, row.getMastery())) {
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

    private static String examKey(ExamResultPublished result, AnswerOutcome answer) {
        return "exam:" + result.attemptId() + ":" + answer.questionId();
    }

    private TopicMastery newRow(UUID userId, Integer topicId, Instant now) {
        return new TopicMastery(
                new TopicMasteryId(userId, topicId), properties.mastery().initial(), now);
    }

    private static MasteryState stateOf(TopicMastery row) {
        return new MasteryState(row.getMastery(), row.getConfidence(), row.getAttempts(), row.getCorrect());
    }

    // The exam module publishes the result as a string (exam.api.ExamResultPublished.AnswerOutcome); once it offers an
    // enum this switch maps that enum instead.
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
