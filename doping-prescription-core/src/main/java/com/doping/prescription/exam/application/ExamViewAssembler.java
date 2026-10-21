package com.doping.prescription.exam.application;

import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.exam.application.ExamViews.AttemptView;
import com.doping.prescription.exam.application.ExamViews.ExamResult;
import com.doping.prescription.exam.application.ExamViews.ExamSummary;
import com.doping.prescription.exam.application.ExamViews.LastResult;
import com.doping.prescription.exam.application.ExamViews.ProfileUpdate;
import com.doping.prescription.exam.application.ExamViews.QuestionResult;
import com.doping.prescription.exam.application.ExamViews.QuestionView;
import com.doping.prescription.exam.application.ExamViews.SavedAnswer;
import com.doping.prescription.exam.application.ExamViews.TopicResult;
import com.doping.prescription.exam.domain.AnswerResult;
import com.doping.prescription.exam.domain.AttemptScore;
import com.doping.prescription.exam.domain.ExamGrader;
import com.doping.prescription.exam.domain.ExamGrader.QuestionKey;
import com.doping.prescription.exam.persistence.Exam;
import com.doping.prescription.exam.persistence.ExamAnswer;
import com.doping.prescription.exam.persistence.ExamAttempt;
import com.doping.prescription.exam.persistence.ExamQuestion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Turns exam entities into the read models of {@link ExamViews}; keeps answer keys out of in-progress views. */
final class ExamViewAssembler {

    static final String PROFILE_PENDING = "PENDING";
    private static final String UNKNOWN_TOPIC = "?";

    private final TopicCatalog topics;

    ExamViewAssembler(TopicCatalog topics) {
        this.topics = topics;
    }

    ExamSummary summary(
            Exam exam, int questionCount, Optional<ExamAttempt> inProgress, Optional<ExamAttempt> lastSubmitted) {
        return new ExamSummary(
                exam.getId(),
                exam.getTitle(),
                exam.getDurationMin(),
                questionCount,
                inProgress.map(ExamAttempt::getId).orElse(null),
                lastSubmitted
                        .flatMap(attempt -> attempt.getScore()
                                .map(score -> new LastResult(
                                        attempt.getId(),
                                        score.net(),
                                        attempt.getSubmittedAt().orElseThrow())))
                        .orElse(null));
    }

    AttemptView attempt(Exam exam, ExamAttempt attempt, List<ExamQuestion> questions, List<ExamAnswer> answers) {
        return new AttemptView(
                attempt.getId(),
                exam.getId(),
                exam.getTitle(),
                exam.getDurationMin(),
                attempt.getStartedAt(),
                questions.stream()
                        .map(question -> new QuestionView(
                                question.getId(),
                                question.getOrderNo(),
                                question.getContent().stem(),
                                question.getContent().options()))
                        .toList(),
                answers.stream()
                        .map(answer -> new SavedAnswer(
                                answer.getId().questionId(),
                                answer.getSelectedKey().orElse(null)))
                        .toList());
    }

    List<QuestionKey> keys(List<ExamQuestion> questions) {
        return questions.stream()
                .map(question -> new QuestionKey(
                        question.getId(),
                        question.getTopicId(),
                        question.getContent().correctKey()))
                .toList();
    }

    ExamResult result(Exam exam, ExamAttempt attempt, List<ExamQuestion> questions, Map<Integer, ExamAnswer> answers) {
        AttemptScore score = attempt.getScore().orElseThrow();
        Map<Integer, Optional<String>> selections = new HashMap<>();
        answers.forEach((questionId, answer) -> selections.put(questionId, answer.getSelectedKey()));
        Map<Integer, TopicCatalog.TopicView> topicsById = topics.topicsById();
        List<TopicResult> byTopic = ExamGrader.grade(keys(questions), selections).topics().stream()
                .map(topic -> new TopicResult(
                        topic.topicId(),
                        topicName(topicsById, topic.topicId()),
                        topic.total(),
                        topic.correct(),
                        topic.wrong(),
                        topic.blank()))
                .toList();
        return new ExamResult(
                attempt.getId(),
                exam.getId(),
                exam.getTitle(),
                score.correct(),
                score.wrong(),
                score.blank(),
                score.net(),
                byTopic,
                questions.stream()
                        .map(question -> questionResult(question, answers))
                        .toList(),
                new ProfileUpdate(PROFILE_PENDING));
    }

    private static QuestionResult questionResult(ExamQuestion question, Map<Integer, ExamAnswer> answers) {
        Optional<ExamAnswer> answer = Optional.ofNullable(answers.get(question.getId()));
        return new QuestionResult(
                question.getId(),
                question.getOrderNo(),
                question.getContent().stem(),
                question.getContent().options(),
                answer.flatMap(ExamAnswer::getSelectedKey).orElse(null),
                question.getContent().correctKey(),
                answer.flatMap(ExamAnswer::getResult).orElse(AnswerResult.BLANK),
                question.getContent().explanation());
    }

    private static String topicName(Map<Integer, TopicCatalog.TopicView> topicsById, Integer topicId) {
        TopicCatalog.TopicView topic = topicsById.get(topicId);
        return topic == null ? UNKNOWN_TOPIC : topic.name();
    }
}
