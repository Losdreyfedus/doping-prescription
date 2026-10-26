package com.doping.prescription.feed.domain;

import static com.doping.prescription.feed.domain.FeedFixtures.FONKSIYONLAR;
import static com.doping.prescription.feed.domain.FeedFixtures.OLASILIK;
import static com.doping.prescription.feed.domain.FeedFixtures.PERKOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.doping.prescription.feed.domain.FeedComposer.Card;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    void aQueueRebuiltMidSessionContinuesTheRowOfTheServedCards() {
        // Given: the session already served two Olasılık cards, then the learner's mastery changed
        FeedContext base = FeedFixtures.context(FeedFixtures.afterScenarioExam());
        FeedContext context = new FeedContext(
                base.topics(),
                base.videos(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                List.of(PERKOM, OLASILIK, OLASILIK),
                base.now(),
                false);

        // When
        List<Card> cards = composer.compose(context, FeedFixtures.quotas(), 10, 1);

        // Then: Olasılık is still the best topic, but a third card in a row would break rule 1
        assertThat(topics(cards).getFirst()).isNotEqualTo(OLASILIK);
        assertThat(cards.getFirst().ranked().breakdown().appliedRules()).isEmpty();
        assertThat(cards.get(1).ranked().candidate().targetTopicId()).isEqualTo(OLASILIK);
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
    void sessionVideosAndRecentlyCompletedVideosAreLeftOut() {
        FeedContext base = FeedFixtures.context(FeedFixtures.afterScenarioExam());
        FeedContext context = new FeedContext(
                base.topics(),
                base.videos(),
                Set.of(),
                Set.of(900),
                Set.of(901),
                Set.of(),
                List.of(),
                base.now(),
                false);

        List<Card> cards = composer.compose(context, FeedFixtures.quotas(), 10, 3);

        assertThat(ids(cards)).doesNotContain(900, 901);
    }

    @Test
    void aDeferredTopicWaitsForTheNextPageWhileTheFirstPageStaysFull() {
        // Given: PerKom was skipped often enough in this session to be deferred (rule 3)
        FeedContext context = deferring(Set.of(PERKOM));

        // When
        List<Card> cards = composer.compose(context, FeedFixtures.quotas(), 10, 3);

        // Then: the first page is full without PerKom, and PerKom comes back from the second page on
        assertThat(cards).hasSize(30);
        assertThat(topics(cards.subList(0, 10))).doesNotContain(PERKOM);
        assertThat(topics(cards.subList(10, 30))).contains(PERKOM);
    }

    @Test
    void aPinnedVideoOfADeferredTopicDoesNotOpenThePage() {
        // Given: 800 is a PerKom video, and PerKom is deferred
        FeedContext context = deferring(Set.of(PERKOM));

        // When
        List<Card> cards = composer.compose(context, FeedFixtures.quotas(), 10, 1, List.of(800, 901));

        // Then
        assertThat(ids(cards).getFirst()).isEqualTo(901);
        assertThat(topics(cards)).hasSize(10).doesNotContain(PERKOM);
    }

    @Test
    void pinnedVideosOpenTheFeedInOrderWithTheirOwnPoolAndReason() {
        List<Card> cards = composer.compose(
                FeedFixtures.context(FeedFixtures.afterScenarioExam()),
                FeedFixtures.quotas(),
                10,
                3,
                List.of(901, 900, 800, 100));

        assertThat(ids(cards).subList(0, 4)).containsExactly(901, 900, 800, 100);
        assertThat(cards.getFirst().reasonText()).isEqualTo("Son denemende Olasılık: 3 soruda 0 doğru");
        assertThat(cards.get(3).ranked().candidate().pool()).isEqualTo(PoolType.POPULAR);
        assertThat(cards.get(3).reasonText()).isEqualTo("Bu hafta en çok izlenenlerden");
        assertThat(cards.getFirst().ranked().breakdown().appliedRules()).containsExactly(FeedComposer.PINNED_NOTE);
        assertThat(ids(cards)).doesNotHaveDuplicates().hasSize(30);
    }

    @Test
    void pinnedVideosAlreadyShownOrMissingAreSkipped() {
        FeedContext base = FeedFixtures.context(FeedFixtures.afterScenarioExam());
        FeedContext context = new FeedContext(
                base.topics(), base.videos(), Set.of(), Set.of(), Set.of(901), Set.of(), List.of(), base.now(), false);

        List<Card> cards = composer.compose(context, FeedFixtures.quotas(), 10, 1, List.of(901, 4242, 900));

        assertThat(ids(cards).getFirst()).isEqualTo(900);
        assertThat(ids(cards)).doesNotContain(901).hasSize(10);
    }

    @Test
    void savedVideosComeInTheGivenOrderWithThePoolOfTheirTopicsBand() {
        // Given: Olasılık is weak, topic 1 medium, Fonksiyonlar unknown; 901 was already shown, 4242 is not LIVE
        FeedContext base = FeedFixtures.context(FeedFixtures.afterScenarioExam());
        FeedContext context = new FeedContext(
                base.topics(), base.videos(), Set.of(), Set.of(), Set.of(901), Set.of(), List.of(), base.now(), false);

        // When
        List<Card> cards = composer.chosen(context, List.of(100, 901, 4242, 900, 1000), 10);

        // Then
        assertThat(ids(cards)).containsExactly(100, 900, 1000);
        assertThat(cards)
                .extracting(card -> card.ranked().candidate().pool())
                .containsExactly(PoolType.POPULAR, PoolType.WEAK_TOPIC, PoolType.EXPLORE);
        assertThat(cards.get(1).reasonText()).isEqualTo("Son denemende Olasılık: 3 soruda 0 doğru");
        assertThat(composer.chosen(context, List.of(100, 900, 1000), 2)).hasSize(2);
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

        List<Card> first = composer.fallback(context, new Random(7), 10);

        assertThat(first).hasSize(10).allSatisfy(card -> {
            assertThat(card.ranked().candidate().pool()).isEqualTo(PoolType.FALLBACK);
            assertThat(card.reasonText()).isEqualTo("Dopingle'da yeni");
        });
        assertThat(ids(composer.fallback(context, new Random(7), 10))).isEqualTo(ids(first));
        assertThat(ids(composer.fallback(context, new Random(8), 10))).isNotEqualTo(ids(first));
    }

    @Test
    void aPoolWithoutAReasonTemplateIsRejectedWhenTheComposerIsBuilt() {
        assertThatThrownBy(() -> new FeedComposer(
                        List.of(Pools.explore()),
                        new WeightedLinearRanker(FeedFixtures.WEIGHTS),
                        List.of(ReasonTemplates.fallback()),
                        2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EXPLORE");
    }

    private static FeedContext deferring(Set<Integer> deferredTopics) {
        FeedContext base = FeedFixtures.context(FeedFixtures.afterScenarioExam());
        return new FeedContext(
                base.topics(),
                base.videos(),
                Set.of(),
                Set.of(),
                Set.of(),
                deferredTopics,
                List.of(),
                base.now(),
                false);
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
