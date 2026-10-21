package com.doping.prescription.points.application;

import com.doping.prescription.points.domain.PointsRules;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration of the points module, {@code prescription.points.*} (docs/algoritma.md Â§7, Â§9).
 *
 * @param correct          a correct answer
 * @param weakTopicCorrect a correct answer on a WEAK_TOPIC card
 * @param streakLength     consecutive correct answers that earn the streak bonus
 * @param streakBonus      the bonus
 * @param dailyCap         most points a learner can earn per calendar day
 */
@Validated
@ConfigurationProperties(prefix = "prescription.points")
public record PointsProperties(
        @Positive int correct,
        @Positive int weakTopicCorrect,
        @Positive int streakLength,
        @Positive int streakBonus,
        @Positive int dailyCap) {

    public PointsRules rules() {
        return new PointsRules(correct, weakTopicCorrect, streakLength, streakBonus, dailyCap);
    }
}
