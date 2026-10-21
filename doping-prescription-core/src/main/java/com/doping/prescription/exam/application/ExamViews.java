package com.doping.prescription.exam.application;

import com.doping.prescription.exam.domain.AnswerResult;
import com.doping.prescription.exam.domain.ExamOption;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read models returned by {@link ExamService} and served as API bodies (docs/api.md, Deneme). Entities never leave the
 * module; answer keys appear only in a submitted attempt's result.
 */
public final class ExamViews {

    private ExamViews() {}

    /** One row of the exam list. */
    public record ExamSummary(
            Integer examId,
            String title,
            int durationMin,
            int questionCount,
            @Schema(nullable = true) UUID inProgressAttemptId,
            @Schema(nullable = true) LastResult lastResult) {}

    /** The learner's latest submitted attempt of an exam. */
    public record LastResult(UUID attemptId, BigDecimal net, Instant submittedAt) {}

    /** An attempt being solved: questions without keys, and the answers saved so far. */
    public record AttemptView(
            UUID attemptId,
            Integer examId,
            String title,
            int durationMin,
            Instant startedAt,
            List<QuestionView> questions,
            List<SavedAnswer> answers) {

        public AttemptView {
            questions = List.copyOf(questions);
            answers = List.copyOf(answers);
        }
    }

    /** A question as shown while solving; no correct key, explanation or topic (docs/api.md). */
    public record QuestionView(Integer questionId, int orderNo, String stem, List<ExamOption> options) {

        public QuestionView {
            options = List.copyOf(options);
        }
    }

    /** @param selectedKey null when the question was cleared */
    public record SavedAnswer(
            Integer questionId, @Schema(nullable = true) String selectedKey) {}

    /** The result screen. */
    public record ExamResult(
            UUID attemptId,
            Integer examId,
            String title,
            int correct,
            int wrong,
            int blank,
            BigDecimal net,
            List<TopicResult> byTopic,
            List<QuestionResult> questions,
            ProfileUpdate profileUpdate) {

        public ExamResult {
            byTopic = List.copyOf(byTopic);
            questions = List.copyOf(questions);
        }
    }

    /** Totals of one topic in the result's topic analysis. */
    public record TopicResult(Integer topicId, String name, int total, int correct, int wrong, int blank) {}

    /** A reviewed question; the correct key and explanation are revealed only here. */
    public record QuestionResult(
            Integer questionId,
            int orderNo,
            String stem,
            List<ExamOption> options,
            @Schema(nullable = true) String selectedKey,
            String correctKey,
            AnswerResult result,
            String explanation) {

        public QuestionResult {
            options = List.copyOf(options);
        }
    }

    /**
     * @param status PENDING until the learner profile has processed this attempt; the web polls the profile
     */
    public record ProfileUpdate(String status) {}
}
