package com.doping.prescription.quiz.api;

import java.util.List;

/** Aggregates over micro-question answers for the admin module. */
public interface QuizStatistics {

    /** Answers and correct answers per micro-question, for questions answered at least once. */
    List<QuestionStats> answersByQuestion();

    record QuestionStats(Integer microQuestionId, long answers, long correct) {}
}
