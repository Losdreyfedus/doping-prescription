package com.doping.prescription.learner.application;

import com.doping.prescription.learner.domain.MasteryBand.BandThresholds;
import com.doping.prescription.learner.domain.MasteryParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration of the learner module, {@code prescription.learner.*} (docs/algoritma.md §9).
 *
 * @param mastery  mastery model coefficients
 * @param cacheTtl lifetime of a cached profile in Redis (docs/mimari.md §5)
 */
@Validated
@ConfigurationProperties(prefix = "prescription.learner")
public record LearnerProperties(
        @NotNull @Valid Mastery mastery, @NotNull Duration cacheTtl) {

    /**
     * @param changeEventThreshold smallest mastery change that publishes learner.mastery-changed
     */
    public record Mastery(
            @NotNull @DecimalMin("0") @DecimalMax("1") Double alpha,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double initial,
            @NotNull @Positive Double confidenceK,
            @NotNull @DecimalMin("0") Double decayLambda,
            @NotNull @Valid SignalWeights signalWeights,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double changeEventThreshold,
            @NotNull @Valid Band band) {

        public MasteryParameters parameters() {
            return new MasteryParameters(
                    alpha,
                    initial,
                    confidenceK,
                    decayLambda,
                    signalWeights.exam(),
                    signalWeights.examBlank(),
                    signalWeights.quiz());
        }
    }

    /** Signal weights {@code w} of docs/algoritma.md §1. */
    public record SignalWeights(
            @NotNull @DecimalMin("0") @DecimalMax("1") Double exam,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double examBlank,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double quiz) {}

    /** Shared band thresholds (docs/algoritma.md §1). */
    public record Band(
            @NotNull @DecimalMin("0") @DecimalMax("1") Double weakThreshold,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double strongThreshold) {

        public BandThresholds thresholds() {
            return new BandThresholds(weakThreshold, strongThreshold);
        }
    }
}
