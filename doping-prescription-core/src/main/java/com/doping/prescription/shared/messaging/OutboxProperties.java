package com.doping.prescription.shared.messaging;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Resubmission of event publications that never completed, e.g. because Kafka was unreachable (ADR 0002).
 *
 * @param resubmitEnabled  whether the running application resubmits them periodically; a restart always does
 * @param resubmitAfter    how old an incomplete publication must be before it is resubmitted
 * @param resubmitInterval how often the outbox is checked
 */
@Validated
@ConfigurationProperties(prefix = "prescription.messaging.outbox")
public record OutboxProperties(
        boolean resubmitEnabled,
        @NotNull Duration resubmitAfter,
        @NotNull Duration resubmitInterval) {}
