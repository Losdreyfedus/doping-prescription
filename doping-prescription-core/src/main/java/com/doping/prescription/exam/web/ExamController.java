package com.doping.prescription.exam.web;

import com.doping.prescription.auth.api.CurrentUser;
import com.doping.prescription.exam.application.ExamService;
import com.doping.prescription.exam.application.ExamViews.AttemptView;
import com.doping.prescription.exam.application.ExamViews.ExamResult;
import com.doping.prescription.exam.application.ExamViews.ExamSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exam endpoints of docs/api.md; the user always comes from the token. */
@RestController
class ExamController {

    private final ExamService exams;

    ExamController(ExamService exams) {
        this.exams = exams;
    }

    @GetMapping("/api/exams")
    List<ExamSummary> exams(CurrentUser user) {
        return exams.listExams(user.userId());
    }

    /** Starts the exam, or returns the attempt already in progress with its answers. */
    @PostMapping("/api/exams/{examId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    AttemptView start(CurrentUser user, @PathVariable Integer examId) {
        return exams.startOrResume(user.userId(), examId);
    }

    @GetMapping("/api/exam-attempts/{attemptId}")
    AttemptView attempt(CurrentUser user, @PathVariable UUID attemptId) {
        return exams.attempt(user.userId(), attemptId);
    }

    @PutMapping("/api/exam-attempts/{attemptId}/answers/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void saveAnswer(
            CurrentUser user,
            @PathVariable UUID attemptId,
            @PathVariable Integer questionId,
            @Valid @RequestBody SaveAnswerRequest request) {
        exams.saveAnswer(user.userId(), attemptId, questionId, request.selectedKey(), request.timeSpentSec());
    }

    @PostMapping("/api/exam-attempts/{attemptId}/submit")
    ExamResult submit(CurrentUser user, @PathVariable UUID attemptId) {
        return exams.submit(user.userId(), attemptId);
    }

    @GetMapping("/api/exam-attempts/{attemptId}/result")
    ExamResult result(CurrentUser user, @PathVariable UUID attemptId) {
        return exams.result(user.userId(), attemptId);
    }

    /**
     * @param selectedKey  A-E, or null to clear the answer
     * @param timeSpentSec seconds spent on the question so far
     */
    record SaveAnswerRequest(
            @Schema(nullable = true) @Pattern(regexp = "[A-E]")
            String selectedKey,

            @Schema(nullable = true) @PositiveOrZero Integer timeSpentSec) {}
}
