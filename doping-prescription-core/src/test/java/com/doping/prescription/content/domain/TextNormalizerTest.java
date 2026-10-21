package com.doping.prescription.content.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextNormalizerTest {

    @ParameterizedTest(name = "[{0}] -> [{1}]")
    @CsvSource(
            delimiter = '|',
            value = {
                "Olasılığın temeli: istenen bölü tüm durumlar | olasiligin temeli istenen bolu tum durumlar",
                "ÜSLÜ ve KÖKLÜ Sayılar | uslu ve koklu sayilar",
                "Işık, İğne, Çiçek, Şeker | isik igne cicek seker",
                "Zar ve para atışında olasılık, 3 örnek | zar ve para atisinda olasilik 3 ornek"
            })
    void foldsTurkishLettersAndPunctuation(String input, String expected) {
        assertThat(TextNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    void collapsesWhitespaceIncludingTabsAndNewlines() {
        assertThat(TextNormalizer.normalize("  çok    boşluk\t ve\nsatır  ")).isEqualTo("cok bosluk ve satir");
    }

    @Test
    void turnsQuotesAndSlashesIntoSpaces() {
        assertThat(TextNormalizer.normalize("Bağımsız olaylar ve \"ve / veya\" kuralı"))
                .isEqualTo("bagimsiz olaylar ve ve veya kurali");
    }

    @Test
    void splitsIntoWords() {
        assertThat(TextNormalizer.tokens("Tanım kümesi, 2 örnek")).containsExactly("tanim", "kumesi", "2", "ornek");
    }

    @Test
    void blankTextHasNoWords() {
        assertThat(TextNormalizer.tokens(" ?! ")).isEmpty();
    }
}
