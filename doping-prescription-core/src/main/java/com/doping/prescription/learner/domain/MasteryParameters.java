package com.doping.prescription.learner.domain;

/**
 * Coefficients of the mastery model (docs/algoritma.md §1, §9), passed in so the domain stays free of Spring.
 *
 * @param alpha           learning rate
 * @param initial         mastery of a topic without signals
 * @param confidenceK     how fast confidence grows with the number of signals
 * @param decayLambda     forgetting rate per day
 * @param examWeight      weight of a correct or wrong exam answer
 * @param examBlankWeight weight of a blank exam answer
 * @param quizWeight      weight of a micro-question answer
 */
public record MasteryParameters(
        double alpha,
        double initial,
        double confidenceK,
        double decayLambda,
        double examWeight,
        double examBlankWeight,
        double quizWeight) {

    public MasteryParameters {
        requireFraction(alpha, "alpha");
        requireFraction(initial, "initial");
        requireFraction(examWeight, "examWeight");
        requireFraction(examBlankWeight, "examBlankWeight");
        requireFraction(quizWeight, "quizWeight");
        if (confidenceK <= 0 || decayLambda < 0) {
            throw new IllegalArgumentException("confidenceK must be positive and decayLambda not negative");
        }
    }

    double weightOf(SignalType type) {
        return switch (type) {
            case EXAM_CORRECT, EXAM_WRONG -> examWeight;
            case EXAM_BLANK -> examBlankWeight;
            case QUIZ_CORRECT, QUIZ_WRONG -> quizWeight;
            case VIDEO_COMPLETED, VIDEO_SKIPPED -> 0;
        };
    }

    private static void requireFraction(double value, String name) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }
}
