package com.doping.prescription.exam.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Exam actions for the admin module's demo personas (docs/api.md, POST /api/admin/demo/persona): personas go through
 * the same service flow as the student, so the event chain is exercised too. Also serves the presenter's answer key.
 */
public interface ExamSubmission {

    /** Opens the exam for the learner or returns the attempt in progress. */
    UUID startOrResume(UUID userId, Integer examId);

    /** Saves the scenario answer of every question (docs/seed-verisi.md); questions without one stay blank. */
    void answerWithScenario(UUID userId, UUID attemptId);

    /** The exam's questions with the correct and the scenario answer, for the admin's answer key card. */
    List<KeyRow> answerKey(Integer examId);

    /**
     * @param scenarioKey the answer given in the demo scenario; empty means left blank
     */
    record KeyRow(int orderNo, Integer questionId, Integer topicId, String correctKey, Optional<String> scenarioKey) {}
}
