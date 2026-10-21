package com.doping.prescription.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Request and response bodies of the auth endpoints (docs/api.md). */
final class AuthDtos {

    private AuthDtos() {}

    /**
     * A user offered on the demo login screen.
     *
     * @param role {@code STUDENT} or {@code ADMIN}
     */
    record DemoUserResponse(String slug, String displayName, String role) {}

    record DemoLoginRequest(@NotBlank @Size(max = 32) String slug) {}

    /** @param token bearer token for the Authorization header */
    record DemoLoginResponse(String token, MeResponse user) {}

    /**
     * The logged-in user.
     *
     * @param examDate exam date used for the exam-proximity weight; null when not set
     */
    record MeResponse(
            UUID userId,
            String slug,
            String displayName,
            String role,
            boolean personalizationEnabled,
            LocalDate examDate) {}
}
