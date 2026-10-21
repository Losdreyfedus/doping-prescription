package com.doping.prescription.quiz.web;

import com.doping.prescription.auth.api.CurrentUser;
import com.doping.prescription.quiz.application.QuizService;
import com.doping.prescription.quiz.application.QuizViews.MicroQuiz;
import com.doping.prescription.quiz.application.QuizViews.QuizResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Micro-question endpoints of docs/api.md; the user always comes from the token. */
@RestController
class QuizController {

    private final QuizService quizzes;

    QuizController(QuizService quizzes) {
        this.quizzes = quizzes;
    }

    /** 200 with the question, 204 if the card has no (more) questions for this learner. */
    @GetMapping("/api/feed/cards/{impressionId}/quiz")
    ResponseEntity<MicroQuiz> quiz(CurrentUser user, @PathVariable UUID impressionId) {
        return quizzes.open(user.userId(), impressionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/api/quiz/{microQuestionId}/answer")
    QuizResult answer(
            CurrentUser user, @PathVariable Integer microQuestionId, @Valid @RequestBody AnswerRequest request) {
        return quizzes.answer(
                user.userId(), microQuestionId, request.quizToken(), request.selectedKey(), request.responseMs());
    }

    /**
     * @param responseMs time from showing the question to answering
     */
    record AnswerRequest(
            @NotBlank String quizToken,
            @NotBlank @Pattern(regexp = "[A-D]") String selectedKey,
            @PositiveOrZero int responseMs) {}
}
