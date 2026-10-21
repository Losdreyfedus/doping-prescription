package com.doping.prescription.content.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KeywordTaggerTest {

    private static final double MIN_CONFIDENCE = 0.5;
    private static final int USLU = 5;
    private static final int KUMELER = 7;
    private static final int OLASILIK = 9;
    private static final int FONKSIYON = 10;

    private final KeywordTagger tagger = new KeywordTagger(MIN_CONFIDENCE);

    @ParameterizedTest(name = "keyword [{0}] vs word [{1}] -> {2}")
    @CsvSource({
        // prefix match: Turkish suffixes
        "kume, kumeler, true",
        "kume, kumesi, true",
        "fonksiyon, fonksiyonlarda, true",
        "olasilik, olasilik, true",
        // final consonant softening k->g, t->d, p->b
        "olasilik, olasiligin, true",
        "kosullu, kosullu, true",
        "kitap, kitabi, true",
        "kanat, kanadi, true",
        // short tokens (< 4 characters) must match whole words
        "us, us, true",
        "us, ustalik, false",
        "zar, zar, true",
        "zar, zarar, false",
        // the keyword must start the word
        "kume, alt kume, false",
        "olasilik, olasi, false"
    })
    void matchesWordByTurkishPrefixRules(String keyword, String word, boolean expected) {
        assertThat(KeywordTagger.tokenMatches(keyword, word)).isEqualTo(expected);
    }

    @Test
    void titleMatchCountsFullWeight() {
        List<TagSuggestion> suggestions =
                tagger.suggest("Kümelerde kesişim", "", List.of(keyword(KUMELER, "kume", 1.0)));

        assertThat(suggestions).containsExactly(new TagSuggestion(KUMELER, 1.0));
    }

    @Test
    void descriptionOnlyMatchCountsHalfWeight() {
        List<TagSuggestion> suggestions =
                tagger.suggest("Küçük bir ipucu", "Kümelerle ilgili", List.of(keyword(KUMELER, "kume", 1.0)));

        assertThat(suggestions).containsExactly(new TagSuggestion(KUMELER, 0.5));
    }

    @Test
    void weightsOfOneTopicAddUpAndAreCappedAtOne() {
        List<TagSuggestion> suggestions = tagger.suggest(
                "Üslü sayılarda üs alma",
                "",
                List.of(keyword(USLU, "uslu", 1.0), keyword(USLU, "us", 0.5), keyword(USLU, "karekok", 0.8)));

        assertThat(suggestions).containsExactly(new TagSuggestion(USLU, 1.0));
    }

    @Test
    void multiWordKeywordNeedsConsecutiveWords() {
        TaggingKeyword domain = keyword(FONKSIYON, "tanim kumesi", 0.7);

        assertThat(tagger.suggest("Tanım kümesini bulma", "", List.of(domain)))
                .containsExactly(new TagSuggestion(FONKSIYON, 0.7));
        assertThat(tagger.suggest("Tanım ve görüntü kümesi", "", List.of(domain)))
                .isEmpty();
    }

    @Test
    void negativeKeywordExcludesTopic() {
        List<TagSuggestion> suggestions = tagger.suggest(
                "Olasılık soruları",
                "Deneme sınavı analizi",
                List.of(keyword(OLASILIK, "olasilik", 1.0), new TaggingKeyword(OLASILIK, "deneme sinavi", 1.0, true)));

        assertThat(suggestions).isEmpty();
    }

    @Test
    void dropsTopicsBelowMinimumConfidence() {
        List<TagSuggestion> suggestions =
                tagger.suggest("Kalan bulma", "", List.of(keyword(3, "kalan", 0.5), keyword(4, "kesir", 0.6)));

        assertThat(suggestions).containsExactly(new TagSuggestion(3, 0.5));
        assertThat(tagger.suggest("Basit bir video", "Kalan sayılar", List.of(keyword(3, "kalan", 0.5))))
                .isEmpty();
    }

    @Test
    void ordersByScoreThenTopicId() {
        List<TagSuggestion> suggestions = tagger.suggest(
                "Kümeler ve fonksiyonlar",
                "Olasılık",
                List.of(
                        keyword(FONKSIYON, "fonksiyon", 1.0),
                        keyword(KUMELER, "kume", 1.0),
                        keyword(OLASILIK, "olasilik", 1.0)));

        assertThat(suggestions).extracting(TagSuggestion::topicId).containsExactly(KUMELER, FONKSIYON, OLASILIK);
        assertThat(suggestions.get(2).score()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new KeywordTagger(1.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TaggingKeyword(1, "kume", 1.2, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TaggingKeyword(1, " ! ", 1.0, false)).isInstanceOf(IllegalArgumentException.class);
    }

    private static TaggingKeyword keyword(int topicId, String keyword, double weight) {
        return new TaggingKeyword(topicId, keyword, weight, false);
    }
}
