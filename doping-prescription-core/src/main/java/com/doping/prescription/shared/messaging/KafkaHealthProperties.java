package com.doping.prescription.shared.messaging;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Kafka health check settings.
 *
 * @param timeout how long the health check waits for the broker before reporting DOWN
 */
@Validated
@ConfigurationProperties(prefix = "prescription.health.kafka")
public record KafkaHealthProperties(@NotNull Duration timeout) {}
