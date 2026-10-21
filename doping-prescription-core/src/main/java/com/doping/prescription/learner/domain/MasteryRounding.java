package com.doping.prescription.learner.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rounds full-precision mastery values for the API and the UI (docs/algoritma.md §1, Hassasiyet).
 *
 * <p>Binary doubles cannot hold most decimals exactly: 0.245 * 0.7 is 0.17149999999999999, which plain HALF_UP would
 * round to 0.171 instead of the specified 0.172. The value is therefore first rounded to {@value #NOISE_SCALE}
 * decimals, which removes the floating-point noise, and then HALF_UP to {@value #API_SCALE}.
 */
public final class MasteryRounding {

    static final int NOISE_SCALE = 9;
    static final int API_SCALE = 3;

    private MasteryRounding() {}

    public static BigDecimal forApi(double value) {
        return BigDecimal.valueOf(value)
                .setScale(NOISE_SCALE, RoundingMode.HALF_UP)
                .setScale(API_SCALE, RoundingMode.HALF_UP);
    }
}
