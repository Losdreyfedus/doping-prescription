package com.doping.prescription.quiz.application;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration of the quiz module, {@code prescription.quiz.*} (docs/algoritma.md §7, §9).
 *
 * @param minWatchRatio  share of the video that must be watched before the question opens
 * @param timeLimitSec   countdown shown with the question
 * @param tokenTtl       lifetime of a quiz token
 * @param minResponseMs  faster answers earn neither points nor a signal
 * @param rateLimitPerMin most answers per learner per minute
 * @param tokenSecret    HMAC key of quiz tokens, separate from the session key; never logged
 */
@Validated
@ConfigurationProperties(prefix = "prescription.quiz")
public record QuizProperties(
        @DecimalMin("0") @DecimalMax("1") double minWatchRatio,
        @Positive int timeLimitSec,
        @NotNull Duration tokenTtl,
        @PositiveOrZero int minResponseMs,
        @Positive int rateLimitPerMin,
        @NotNull @Size(min = 32) String tokenSecret) {

    @Override
    public String toString() {
        return "QuizProperties[minWatchRatio=" + minWatchRatio + ", tokenTtl=" + tokenTtl + "]";
    }
}
