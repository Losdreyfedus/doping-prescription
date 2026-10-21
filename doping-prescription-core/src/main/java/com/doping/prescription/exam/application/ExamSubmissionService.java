package com.doping.prescription.exam.application;

import com.doping.prescription.exam.api.ExamSubmission;
import com.doping.prescription.exam.persistence.ExamAttempt;
import com.doping.prescription.exam.persistence.ExamAttemptRepository;
import com.doping.prescription.exam.persistence.ExamQuestion;
import com.doping.prescription.exam.persistence.ExamQuestionRepository;
import com.doping.prescription.shared.error.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves {@link ExamSubmission} through {@link ExamService}, the same path the student's requests take. */
@Service
class ExamSubmissionService implements ExamSubmission {

    private static final int SCENARIO_TIME_SPENT_SEC = 60;

    private final ExamService exams;
    private final ExamAttemptRepository attempts;
    private final ExamQuestionRepository questions;

    ExamSubmissionService(ExamService exams, ExamAttemptRepository attempts, ExamQuestionRepository questions) {
        this.exams = exams;
        this.attempts = attempts;
        this.questions = questions;
    }

    @Override
    public UUID startOrResume(UUID userId, Integer examId) {
        return exams.startOrResume(userId, examId).attemptId();
    }

    @Override
    @Transactional
    public void answerWithScenario(UUID userId, UUID attemptId) {
        ExamAttempt attempt = attempts.findById(attemptId)
                .filter(candidate -> candidate.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Deneme bulunamadı."));
        for (ExamQuestion question : questions.findByExamIdOrderByOrderNo(attempt.getExamId())) {
            exams.saveAnswer(
                    userId,
                    attemptId,
                    question.getId(),
                    question.getScenarioAnswer(),
                    question.getScenarioAnswer().isPresent() ? SCENARIO_TIME_SPENT_SEC : null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeyRow> answerKey(Integer examId) {
        return questions.findByExamIdOrderByOrderNo(examId).stream()
                .map(question -> new KeyRow(
                        question.getOrderNo(),
                        question.getId(),
                        question.getTopicId(),
                        question.getContent().correctKey(),
                        question.getScenarioAnswer()))
                .toList();
    }
}
