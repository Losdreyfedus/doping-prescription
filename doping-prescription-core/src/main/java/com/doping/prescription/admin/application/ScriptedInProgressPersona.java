package com.doping.prescription.admin.application;

import com.doping.prescription.admin.domain.DemoPersona;
import com.doping.prescription.exam.api.ExamSubmission;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * {@code SCRIPTED_IN_PROGRESS} (docs/seed-verisi.md): Mini Deneme 1 started with every scenario answer marked, not
 * submitted. On stage the presenter only presses "Sınavı Bitir".
 */
@Component
class ScriptedInProgressPersona implements DemoPersona {

    static final int MINI_EXAM_1 = 1;

    private final ExamSubmission exams;

    ScriptedInProgressPersona(ExamSubmission exams) {
        this.exams = exams;
    }

    @Override
    public String name() {
        return "SCRIPTED_IN_PROGRESS";
    }

    @Override
    public void apply(UUID userId) {
        exams.answerWithScenario(userId, exams.startOrResume(userId, MINI_EXAM_1));
    }
}
