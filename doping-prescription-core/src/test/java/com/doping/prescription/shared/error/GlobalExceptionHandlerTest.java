package com.doping.prescription.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private final MockMvcTester mvc = MockMvcTester.create(MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestIdFilter())
            .build());

    @Test
    void domainErrorBecomesProblemDetailWithCode() {
        MvcTestResult result = mvc.get().uri("/not-found").exchange();

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Deneme bulunamadı.");
    }

    @Test
    void validationErrorListsInvalidFields() {
        MvcTestResult result = mvc.post()
                .uri("/validated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\": \"\"}")
                .exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REQUEST");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("slug");
    }

    @Test
    void unreadableBodyIsInvalidRequest() {
        MvcTestResult result = mvc.post()
                .uri("/validated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json")
                .exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void unexpectedErrorRevealsNoDetails() {
        MvcTestResult result = mvc.get().uri("/boom").exchange();

        assertThat(result).hasStatus(500);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INTERNAL_ERROR");
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(GlobalExceptionHandler.UNEXPECTED_DETAIL);
        assertThat(result).bodyText().doesNotContain("secret internal state");
    }

    @Test
    void echoesSafeRequestIdAndReplacesUnsafeOne() {
        MvcTestResult safe = mvc.get()
                .uri("/not-found")
                .header(RequestIdFilter.HEADER, "abc-123")
                .exchange();
        MvcTestResult unsafe = mvc.get()
                .uri("/not-found")
                .header(RequestIdFilter.HEADER, "x\ny")
                .exchange();

        assertThat(safe.getResponse().getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123");
        assertThat(unsafe.getResponse().getHeader(RequestIdFilter.HEADER))
                .isNotEqualTo("x\ny")
                .hasSize(36);
    }

    @RestController
    static class FailingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Deneme bulunamadı.");
        }

        @PostMapping("/validated")
        void validated(@Valid @RequestBody Body body) {
            // validation happens before the method runs
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret internal state");
        }

        record Body(@NotBlank String slug) {}
    }
}
