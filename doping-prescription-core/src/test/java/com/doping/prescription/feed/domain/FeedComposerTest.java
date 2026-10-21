package com.doping.prescription.feed.domain;

import static com.doping.prescription.feed.domain.FeedFixtures.FONKSIYONLAR;
import static com.doping.prescription.feed.domain.FeedFixtures.OLASILIK;
import static com.doping.prescription.feed.domain.FeedFixtures.PERKOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.doping.prescription.feed.domain.FeedComposer.Card;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FeedComposerTest {

    private final FeedComposer composer = FeedFixtures.composer();

    @Test
    void afterTheScenarioExamTheFeedPrescribesTheWeakTopics() {
        List<Card> cards =
                composer.compose(FeedFixtures.context(FeedFixtures.afterScenarioExam()), FeedFixtures.quotas(), 10, 3);

        List<Integer> topics = topics(cards);
        assertThat(topics.getFirst()).isEqualTo(OLASILIK);
        assertThat(cards.getFirst().reasonText()).isEqualTo("Son denemende Olasılık: 3 soruda 0 doğru");
        assertThat(topics.subList(0, 5).stream().filter(topic -> topic == OLASILIK || topic == PERKOM))
                .hasSizeGreaterThanOrEqualTo(3);
        assertThat(cards.subList(0, 10))
                .anySatisfy(card -> assertThat(card.reasonText()).isEqualTo("TYT'de sık çıkan konu: Fonksiyonlar"));
        assertThat(cards.stream().map(card -> card.ranked().candidate().video().contentId()))
                .doesNotHaveDuplicates();
    }

    @Test
    void noTopicAppearsMoreThanTwiceInARow() {
        List<Integer> topics = topics(
                composer.compose(FeedFixtures.context(FeedFixtures.afterScenarioExam()), FeedFixtures.quotas(), 10, 3));

        for (int index = 2; index < topics.size(); index++) {
            assertThat(topics.subList(index - 2, index + 1).stream().distinct())
                    .as("cards %d-%d", index - 2, index)
                    .hasSizeGreaterThan(1);
        }
    }

    @Test
    void scoresFollowTheDocumentedWeakness() {
        Card first = composer.compose(
                        FeedFixtures.context(FeedFixtures.afterScenarioExam()), FeedFixtures.quotas(), 10, 1)
                .getFirst();

        assertThat(first.ranked().breakdown().weakness()).isCloseTo((1 - 0.1715) * 0.4512, within(1e-9));
    }

    @Test
    void aLearnerWithoutSignalsGetsExploreAndPopularCards() {
        Map<PoolType, Double> coldStart = new EnumMap<>(PoolType.class);
        coldStart.put(PoolType.EXPLORE, 0.7);
        coldStart.put(PoolType.POPULAR, 0.3);

        List<Card> cards = composer.compose(FeedFixtures.context(FeedFixtures.noSignals()), coldStart, 10, 1);

        assertThat(cards).hasSize(10);
        assertThat(cards)
                .extracting(card -> card.ranked().candidate().pool())
                .containsOnly(PoolType.EXPLORE, PoolType.POPULAR);
        assertThat(cards.getFirst().ranked().candidate().targetTopicId()).isEqualTo(FONKSIYONLAR);
    }

    @Test
    void sessionVideosRecentlyCompletedVideosAndDeferredTopicsAreLeftOut() {
        FeedContext base = FeedFixtures.context(FeedFixtures.afterScenarioExam());
        FeedContext context = new FeedContext(
                base.topics(), base.videos(), Set.of(), Set.of(900), Set.of(901), Set.of(PERKOM), base.now(), false);

        List<Card> cards = composer.compose(context, FeedFixtures.quotas(), 10, 3);

        assertThat(cards)
                .extracting(card -> card.ranked().candidate().video().contentId())
                .doesNotContain(900, 901);
        assertThat(topics(cards)).doesNotContain(PERKOM);
    }

    @Test
    void topicOnlyReturnsTheTopicsVideosBestFirst() {
        List<Card> cards = composer.topicOnly(FeedFixtures.context(FeedFixtures.afterScenarioExam()), OLASILIK, 10);

        assertThat(topics(cards)).hasSize(4).containsOnly(OLASILIK);
        assertThat(cards).extracting(card -> card.ranked().score()).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void fallbackIsTheSameForTheSameSeedAndDifferentForAnother() {
        FeedContext context = FeedFixtures.context(FeedFixtures.afterScenarioExam());

        List<Card> first = composer.fallback(context, 7, 10);

        assertThat(first).hasSize(10).allSatisfy(card -> {
            assertThat(card.ranked().candidate().pool()).isEqualTo(PoolType.FALLBACK);
            assertThat(card.reasonText()).isEqualTo("Dopingle'da yeni");
        });
        assertThat(ids(composer.fallback(context, 7, 10))).isEqualTo(ids(first));
        assertThat(ids(composer.fallback(context, 8, 10))).isNotEqualTo(ids(first));
    }

    private static List<Integer> topics(List<Card> cards) {
        return cards.stream()
                .map(card -> card.ranked().candidate().targetTopicId())
                .toList();
    }

    private static List<Integer> ids(List<Card> cards) {
        return cards.stream()
                .map(card -> card.ranked().candidate().video().contentId())
                .toList();
    }
}
