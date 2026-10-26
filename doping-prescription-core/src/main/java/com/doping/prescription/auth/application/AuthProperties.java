package com.doping.prescription.auth.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration of the auth module, {@code prescription.auth.*}.
 *
 * @param jwtSecret         HS256 signing key; from PRESCRIPTION_JWT_SECRET, never logged. At least 32 characters
 * @param tokenTtl          lifetime of a session token
 * @param corsAllowedOrigin the web application's origin (docs/mimari.md §6)
 * @param demoLoginEnabled  whether the passwordless demo login endpoints exist; on only in the demo profile
 */
@Validated
@ConfigurationProperties(prefix = "prescription.auth")
public record AuthProperties(
        @NotBlank @Size(min = 32) String jwtSecret,
        @NotNull Duration tokenTtl,
        @NotBlank String corsAllowedOrigin,
        boolean demoLoginEnabled) {

    /** The property that switches the demo login endpoints on. */
    public static final String DEMO_LOGIN_ENABLED = "prescription.auth.demo-login-enabled";

    @Override
    public String toString() {
        // keep the secret out of logs and actuator output
        return "AuthProperties[tokenTtl=" + tokenTtl + ", corsAllowedOrigin=" + corsAllowedOrigin
                + ", demoLoginEnabled=" + demoLoginEnabled + "]";
    }
}
