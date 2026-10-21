package com.doping.prescription.quiz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.quiz.domain.OptionShuffle.Shuffled;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class OptionShuffleTest {

    private static final Map<String, String> OPTIONS =
            new LinkedHashMap<>(Map.of("A", "1/6", "B", "1/3", "C", "1/2", "D", "2/3"));

    @Test
    void relabelsTheShuffledOptionsAndKeepsTheirTexts() {
        Shuffled shuffled = OptionShuffle.shuffle(OPTIONS, new Random(3));

        assertThat(shuffled.shown().keySet()).containsExactly("A", "B", "C", "D");
        shuffled.shownToStored()
                .forEach((shown, stored) ->
                        assertThat(shuffled.shown().get(shown)).isEqualTo(OPTIONS.get(stored)));
        assertThat(shuffled.shownToStored().values()).containsExactlyInAnyOrder("A", "B", "C", "D");
    }

    @Test
    void findsTheShownLetterOfTheCorrectOption() {
        Shuffled shuffled = OptionShuffle.shuffle(OPTIONS, new Random(3));

        String shownCorrect = Shuffled.shownKeyOf(shuffled.shownToStored(), "B");

        assertThat(shuffled.shown().get(shownCorrect)).isEqualTo("1/3");
    }

    @Test
    void differentSeedsGiveDifferentOrders() {
        assertThat(OptionShuffle.shuffle(OPTIONS, new Random(1)).shownToStored())
                .isNotEqualTo(OptionShuffle.shuffle(OPTIONS, new Random(2)).shownToStored());
    }
}
