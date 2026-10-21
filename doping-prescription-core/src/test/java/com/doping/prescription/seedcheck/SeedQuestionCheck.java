package com.doping.prescription.seedcheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies seed questions against their independent {@code check} expressions (docs/seed-verisi.md): the expression's
 * value must equal the correct option and no other option may have the same value, so every question has exactly one
 * correct answer. Questions marked {@code {"manual": "..."}} are reported, not computed.
 */
public final class SeedQuestionCheck {

    private static final String OPTION_KEYS = "ABCDE";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private SeedQuestionCheck() {}

    /** Reads a seed file from the classpath, e.g. {@code seed/micro-questions.json}. */
    public static JsonNode readSeedFile(String path) {
        try (InputStream json = SeedQuestionCheck.class.getClassLoader().getResourceAsStream(path)) {
            if (json == null) {
                throw new IllegalArgumentException("Seed file not found: " + path);
            }
            return JSON.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<String> optionTexts(JsonNode question) {
        List<String> options = new ArrayList<>();
        question.get("options").forEach(option -> options.add(option.asString()));
        return options;
    }

    /**
     * Verifies one question and returns its manual-check reason, if it has one instead of an expression.
     *
     * @param label identifies the question in assertion messages
     */
    public static Optional<String> verify(String label, JsonNode question) {
        JsonNode check = question.get("check");
        assertThat(check).as("%s has a check", label).isNotNull();
        if (check.isObject()) {
            String reason = check.get("manual").asString();
            assertThat(reason).as("%s manual check reason", label).isNotBlank();
            return Optional.of(reason);
        }
        verifyExpression(
                label,
                check.asString(),
                optionTexts(question),
                question.get("correctKey").asString());
        return Optional.empty();
    }

    static void verifyExpression(String label, String expression, List<String> options, String correctKey) {
        Rational expected = RationalExpression.evaluate(expression);
        int correctIndex = OPTION_KEYS.indexOf(correctKey);
        for (int index = 0; index < options.size(); index++) {
            Rational optionValue = RationalExpression.parseOption(options.get(index));
            char key = OPTION_KEYS.charAt(index);
            if (index == correctIndex) {
                assertThat(optionValue)
                        .as("%s: correct option %s (%s) equals check %s", label, key, options.get(index), expression)
                        .isEqualTo(expected);
            } else {
                assertThat(optionValue)
                        .as(
                                "%s: option %s (%s) must not also equal check %s",
                                label, key, options.get(index), expression)
                        .isNotEqualTo(expected);
            }
        }
    }
}
