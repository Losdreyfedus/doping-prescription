package com.doping.prescription.admin.web;

import com.doping.prescription.admin.application.ContentEffectivenessService;
import com.doping.prescription.admin.application.ContentEffectivenessService.VideoEffectiveness;
import com.doping.prescription.admin.application.DemoControlService;
import com.doping.prescription.admin.application.DemoControlService.AnswerKeyRow;
import com.doping.prescription.admin.application.DemoControlService.FeedDebug;
import com.doping.prescription.admin.application.DemoControlService.StudentCard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Demo control endpoints of docs/api.md (Admin); ADMIN only, enforced by the security rules on /api/admin/**. */
@RestController
class AdminDemoController {

    private final DemoControlService demo;
    private final ContentEffectivenessService effectiveness;

    AdminDemoController(DemoControlService demo, ContentEffectivenessService effectiveness) {
        this.demo = demo;
        this.effectiveness = effectiveness;
    }

    @GetMapping("/api/admin/demo/students")
    List<StudentCard> students() {
        return demo.students();
    }

    @GetMapping("/api/admin/demo/personas")
    List<String> personas() {
        return demo.personaNames();
    }

    @PostMapping("/api/admin/demo/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reset(@Valid @RequestBody ResetRequest request) {
        demo.reset(request.slug());
    }

    @PostMapping("/api/admin/demo/persona")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void persona(@Valid @RequestBody PersonaRequest request) {
        demo.loadPersona(request.slug(), request.persona());
    }

    @GetMapping("/api/admin/demo/answer-key")
    List<AnswerKeyRow> answerKey() {
        return demo.answerKey();
    }

    /** Answers and correct-answer rate of each video's micro-questions, best first. */
    @GetMapping("/api/admin/content-effectiveness")
    List<VideoEffectiveness> contentEffectiveness() {
        return effectiveness.byVideo();
    }

    /** docs/demo-senaryosu.md, step 7: called from Swagger UI with an admin token. */
    @GetMapping("/api/admin/feed-debug")
    FeedDebug feedDebug(@RequestParam String slug) {
        return demo.feedDebug(slug);
    }

    record ResetRequest(@NotBlank String slug) {}

    /**
     * @param persona NEW or SCRIPTED_IN_PROGRESS (see GET /api/admin/demo/personas)
     */
    record PersonaRequest(@NotBlank String slug, @NotBlank String persona) {}
}
