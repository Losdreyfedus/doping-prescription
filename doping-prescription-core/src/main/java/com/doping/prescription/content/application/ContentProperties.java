package com.doping.prescription.content.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration of the content module, {@code prescription.content.*} (docs/algoritma.md §9).
 *
 * @param tagging keyword tagging settings
 */
@Validated
@ConfigurationProperties(prefix = "prescription.content")
public record ContentProperties(@NotNull @Valid Tagging tagging) {

    /**
     * @param minConfidence lowest keyword score that becomes a topic suggestion
     */
    public record Tagging(
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double minConfidence) {}
}
