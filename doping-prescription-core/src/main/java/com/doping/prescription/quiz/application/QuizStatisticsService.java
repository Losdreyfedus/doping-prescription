package com.doping.prescription.quiz.application;

import com.doping.prescription.quiz.api.QuizStatistics;
import com.doping.prescription.quiz.persistence.MicroQuizAttemptRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves {@link QuizStatistics} with one aggregate query. */
@Service
class QuizStatisticsService implements QuizStatistics {

    private final MicroQuizAttemptRepository attempts;

    QuizStatisticsService(MicroQuizAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionStats> answersByQuestion() {
        return attempts.countByQuestion();
    }
}
