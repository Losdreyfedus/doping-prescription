package com.doping.prescription.exam.application;

import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.exam.application.ExamViews.AttemptView;
import com.doping.prescription.exam.application.ExamViews.ExamResult;
import com.doping.prescription.exam.application.ExamViews.ExamSummary;
import com.doping.prescription.exam.domain.AttemptStatus;
import com.doping.prescription.exam.domain.ExamGrader;
import com.doping.prescription.exam.domain.ExamGrader.GradedExam;
import com.doping.prescription.exam.domain.ExamGrader.GradedQuestion;
import com.doping.prescription.exam.persistence.Exam;
import com.doping.prescription.exam.persistence.ExamAnswer;
import com.doping.prescription.exam.persistence.ExamAnswerId;
import com.doping.prescription.exam.persistence.ExamAnswerRepository;
import com.doping.prescription.exam.persistence.ExamAttempt;
import com.doping.prescription.exam.persistence.ExamAttemptRepository;
import com.doping.prescription.exam.persistence.ExamQuestion;
import com.doping.prescription.exam.persistence.ExamQuestionRepository;
import com.doping.prescription.exam.persistence.ExamRepository;
import com.doping.prescription.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exam use cases: list, start or resume, answer, submit and read the result (docs/api.md, Deneme). */
@Service
public class ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    static final String EXAM_NOT_FOUND = "Deneme bulunamadı.";
    static final String ATTEMPT_NOT_FOUND = "Deneme oturumu bulunamadı.";
    static final String QUESTION_NOT_FOUND = "Soru bu denemede yok.";
    static final String RESULT_NOT_READY = "Bu denemenin sonucu henüz yok.";

    private final ExamRepository exams;
    private final ExamQuestionRepository questions;
    private final ExamAttemptRepository attempts;
    private final ExamAnswerRepository answers;
    private final ExamViewAssembler views;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ExamService(
            ExamRepository exams,
            ExamQuestionRepository questions,
            ExamAttemptRepository attempts,
            ExamAnswerRepository answers,
            TopicCatalog topics,
            ApplicationEventPublisher events,
            Clock clock) {
        this.exams = exams;
        this.questions = questions;
        this.attempts = attempts;
        this.answers = answers;
        this.views = new ExamViewAssembler(topics);
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ExamSummary> listExams(UUID userId) {
        return exams.findByActiveTrueOrderById().stream()
                .map(exam -> views.summary(
                        exam,
                        Math.toIntExact(questions.countByExamId(exam.getId())),
                        attempts.findFirstByUserIdAndExamIdAndStatus(userId, exam.getId(), AttemptStatus.IN_PROGRESS),
                        attempts.findFirstByUserIdAndExamIdAndStatusOrderBySubmittedAtDesc(
                                userId, exam.getId(), AttemptStatus.SUBMITTED)))
                .toList();
    }

    /** Opens a new attempt, or returns the learner's attempt in progress with its saved answers. */
    @Transactional
    public AttemptView startOrResume(UUID userId, Integer examId) {
        Exam exam =
                exams.findById(examId).filter(Exam::isActive).orElseThrow(() -> new NotFoundException(EXAM_NOT_FOUND));
        ExamAttempt attempt = attempts.findFirstByUserIdAndExamIdAndStatus(userId, examId, AttemptStatus.IN_PROGRESS)
                .orElseGet(() ->
                        attempts.saveAndFlush(new ExamAttempt(UUID.randomUUID(), userId, examId, clock.instant())));
        return attemptView(exam, attempt);
    }

    @Transactional(readOnly = true)
    public AttemptView attempt(UUID userId, UUID attemptId) {
        ExamAttempt attempt = owned(userId, attemptId);
        return attemptView(exam(attempt.getExamId()), attempt);
    }

    /** Saves or clears ({@code selectedKey} empty) one answer of an attempt in progress. */
    @Transactional
    public void saveAnswer(
            UUID userId, UUID attemptId, Integer questionId, Optional<String> selectedKey, Integer timeSpentSec) {
        ExamAttempt attempt = inProgress(owned(userId, attemptId));
        ExamQuestion question = questions
                .findById(questionId)
                .filter(candidate -> candidate.getExamId().equals(attempt.getExamId()))
                .orElseThrow(() -> new NotFoundException(QUESTION_NOT_FOUND));
        ExamAnswerId id = new ExamAnswerId(attempt.getId(), question.getId());
        Instant now = clock.instant();
        answers.findById(id)
                .ifPresentOrElse(
                        answer -> answer.change(selectedKey.orElse(null), timeSpentSec, now),
                        () -> answers.save(new ExamAnswer(id, selectedKey.orElse(null), timeSpentSec, now)));
    }

    /**
     * Grades the attempt, closes it and publishes {@link ExamResultPublished} through the outbox in the same
     * transaction. A second submit, also a concurrent one, fails with {@link AttemptAlreadySubmittedException}.
     */
    @Transactional
    public ExamResult submit(UUID userId, UUID attemptId) {
        ExamAttempt attempt = inProgress(owned(userId, attemptId));
        List<ExamQuestion> examQuestions = questions.findByExamIdOrderByOrderNo(attempt.getExamId());
        Map<Integer, ExamAnswer> saved = answersByQuestion(attempt.getId());
        GradedExam graded = ExamGrader.grade(views.keys(examQuestions), selections(saved));
        Instant now = clock.instant();
        storeGrades(attempt.getId(), graded, saved, now);
        attempt.submit(graded.score(), now);
        try {
            attempts.saveAndFlush(attempt);
        } catch (ObjectOptimisticLockingFailureException concurrentSubmit) {
            throw new AttemptAlreadySubmittedException();
        }
        events.publishEvent(resultEvent(attempt, graded, saved, now));
        log.info("Exam submitted attemptId={} examId={}", attempt.getId(), attempt.getExamId());
        return views.result(exam(attempt.getExamId()), attempt, examQuestions, answersByQuestion(attempt.getId()));
    }

    @Transactional(readOnly = true)
    public ExamResult result(UUID userId, UUID attemptId) {
        ExamAttempt attempt = owned(userId, attemptId);
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new NotFoundException(RESULT_NOT_READY);
        }
        return views.result(
                exam(attempt.getExamId()),
                attempt,
                questions.findByExamIdOrderByOrderNo(attempt.getExamId()),
                answersByQuestion(attempt.getId()));
    }

    private void storeGrades(UUID attemptId, GradedExam graded, Map<Integer, ExamAnswer> saved, Instant now) {
        for (GradedQuestion question : graded.questions()) {
            ExamAnswer answer = saved.computeIfAbsent(
                    question.questionId(),
                    questionId ->
                            answers.save(new ExamAnswer(new ExamAnswerId(attemptId, questionId), null, null, now)));
            answer.grade(question.result(), now);
        }
    }

    private ExamResultPublished resultEvent(
            ExamAttempt attempt, GradedExam graded, Map<Integer, ExamAnswer> saved, Instant now) {
        List<ExamResultPublished.AnswerOutcome> outcomes = graded.questions().stream()
                .map(question -> new ExamResultPublished.AnswerOutcome(
                        question.questionId(),
                        question.topicId(),
                        question.result().name(),
                        saved.get(question.questionId()).getTimeSpentSec().orElse(null)))
                .toList();
        return new ExamResultPublished(
                UUID.randomUUID(),
                ExamResultPublished.SCHEMA_VERSION,
                now,
                attempt.getUserId(),
                attempt.getId(),
                attempt.getExamId(),
                outcomes);
    }

    private AttemptView attemptView(Exam exam, ExamAttempt attempt) {
        return views.attempt(
                exam,
                attempt,
                questions.findByExamIdOrderByOrderNo(exam.getId()),
                answers.findByIdAttemptId(attempt.getId()));
    }

    /** A record of another learner answers 404, exactly like a missing one (docs/mimari.md §6). */
    private ExamAttempt owned(UUID userId, UUID attemptId) {
        return attempts.findById(attemptId)
                .filter(attempt -> attempt.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException(ATTEMPT_NOT_FOUND));
    }

    private static ExamAttempt inProgress(ExamAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new AttemptAlreadySubmittedException();
        }
        return attempt;
    }

    private Exam exam(Integer examId) {
        return exams.findById(examId).orElseThrow(() -> new NotFoundException(EXAM_NOT_FOUND));
    }

    private Map<Integer, ExamAnswer> answersByQuestion(UUID attemptId) {
        return answers.findByIdAttemptId(attemptId).stream()
                .collect(Collectors.toMap(
                        answer -> answer.getId().questionId(), Function.identity(), (a, b) -> a, HashMap::new));
    }

    private static Map<Integer, Optional<String>> selections(Map<Integer, ExamAnswer> saved) {
        Map<Integer, Optional<String>> selections = new HashMap<>();
        saved.forEach((questionId, answer) -> selections.put(questionId, answer.getSelectedKey()));
        return selections;
    }
}
