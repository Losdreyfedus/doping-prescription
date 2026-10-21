package com.doping.prescription.auth.web;

import com.doping.prescription.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
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
     * @param role the user's role
     */
    record DemoUserResponse(String slug, String displayName, Role role) {}

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
            Role role,
            boolean personalizationEnabled,
            @Schema(nullable = true) LocalDate examDate) {}
}
