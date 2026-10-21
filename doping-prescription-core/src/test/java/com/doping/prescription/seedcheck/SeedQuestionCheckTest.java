package com.doping.prescription.seedcheck;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SeedQuestionCheckTest {

    private static final List<String> OPTIONS = List.of("1/6", "1/3", "1/2", "2/3");

    @Test
    void acceptsQuestionWhoseCheckMatchesOnlyTheCorrectOption() {
        assertThatCode(() -> SeedQuestionCheck.verifyExpression("ok", "1/(6/2)", OPTIONS, "B"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWrongAnswerKey() {
        assertThatThrownBy(() -> SeedQuestionCheck.verifyExpression("wrong key", "1/(6/2)", OPTIONS, "C"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void rejectsQuestionWithTwoCorrectOptions() {
        List<String> duplicateValue = List.of("1/6", "1/3", "2/6", "2/3");

        assertThatThrownBy(() -> SeedQuestionCheck.verifyExpression("two answers", "1/3", duplicateValue, "B"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("must not also equal");
    }

    @Test
    void rejectsNonNumericOptionWhenCheckIsNumeric() {
        List<String> symbolic = List.of("2√2", "1/3", "1/2", "2/3");

        assertThatThrownBy(() -> SeedQuestionCheck.verifyExpression("symbolic", "1/3", symbolic, "B"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
