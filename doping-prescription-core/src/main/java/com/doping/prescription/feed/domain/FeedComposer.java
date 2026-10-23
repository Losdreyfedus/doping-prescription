package com.doping.prescription.feed.domain;

import com.doping.prescription.feed.domain.FeedRanker.Ranked;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds feed pages from the pools (docs/algoritma.md §2-4): quotas per page, ranking, then the diversity rules applied
 * greedily. Pure and deterministic.
 */
public final class FeedComposer {

    static final String PINNED_NOTE = "pinned by prescription.feed.pinned-content-ids";

    private final Map<PoolType, CandidatePool> pools;
    private final FeedRanker ranker;
    private final Map<PoolType, ReasonTemplate> templates;
    private final int maxSameTopicInRow;

    public FeedComposer(
            List<CandidatePool> pools, FeedRanker ranker, List<ReasonTemplate> templates, int maxSameTopicInRow) {
        this.pools = pools.stream().collect(Collectors.toMap(CandidatePool::type, Function.identity()));
        this.ranker = Objects.requireNonNull(ranker, "ranker");
        this.templates = templates.stream().collect(Collectors.toMap(ReasonTemplate::pool, Function.identity()));
        this.maxSameTopicInRow = maxSameTopicInRow;
    }

    public String algoVersion() {
        return ranker.version();
    }

    /** {@code pages} consecutive pages; a video appears at most once, and rule 1 holds across page boundaries. */
    public List<Card> compose(FeedContext context, Map<PoolType, Double> quotas, int pageSize, int pages) {
        return compose(context, quotas, pageSize, pages, List.of());
    }

    /**
     * Like {@link #compose(FeedContext, Map, int, int)}, but the pinned videos open the first page in the given order
     * (docs/algoritma.md §2, öne çıkarılan içerik). Each keeps the pool, score and reason it would have anyway; videos
     * that are not LIVE or were already shown in the session are skipped.
     */
    public List<Card> compose(
            FeedContext context, Map<PoolType, Double> quotas, int pageSize, int pages, List<Integer> pinned) {
        List<Card> cards = new ArrayList<>(pinnedCards(context, quotas, pinned, pageSize));
        Set<Integer> chosen = new HashSet<>(context.excluded());
        cards.forEach(card -> chosen.add(card.ranked().candidate().video().contentId()));
        for (int page = 0; page < pages; page++) {
            int size = page == 0 ? pageSize - cards.size() : pageSize;
            if (size <= 0) {
                continue;
            }
            List<Card> next = page(context, quotas, size, chosen, cards);
            if (next.isEmpty()) {
                break;
            }
            next.forEach(card -> chosen.add(card.ranked().candidate().video().contentId()));
            cards.addAll(next);
        }
        return cards;
    }

    private List<Card> pinnedCards(FeedContext context, Map<PoolType, Double> quotas, List<Integer> pinned, int limit) {
        List<Card> cards = new ArrayList<>();
        for (Integer contentId : pinned) {
            if (cards.size() == limit || context.excluded().contains(contentId)) {
                continue;
            }
            // the first pool (in quota order) that offers the video decides its pool and reason
            quotas.keySet().stream()
                    .map(pools::get)
                    .filter(Objects::nonNull)
                    .flatMap(pool -> pool.candidates(context).stream())
                    .filter(candidate -> candidate.video().contentId().equals(contentId))
                    .findFirst()
                    .ifPresent(candidate ->
                            cards.add(card(ranker.rank(candidate, context), List.of(PINNED_NOTE), context)));
        }
        return cards;
    }

    /** All cards of a single topic, best first (Eksiklerim → Çalış); the diversity rule does not apply. */
    public List<Card> topicOnly(FeedContext context, Integer topicId, int size) {
        FeedContext.TopicState topic = context.topic(topicId);
        PoolType pool = switch (topic.band()) {
            case WEAK -> PoolType.WEAK_TOPIC;
            case UNKNOWN -> PoolType.EXPLORE;
            default -> PoolType.POPULAR;
        };
        return context.videos().stream()
                .filter(video -> video.topicId().equals(topicId))
                .filter(video -> !context.excluded().contains(video.contentId()))
                .map(video -> ranker.rank(new Candidate(video, pool, topicId), context))
                .sorted(Comparator.comparingDouble(Ranked::score).reversed())
                .limit(size)
                .map(ranked -> card(ranked, List.of(), context))
                .toList();
    }

    /** Personalization off: every LIVE video in an order seeded by the session (docs/algoritma.md §2). */
    public List<Card> fallback(FeedContext context, long seed, int size) {
        List<FeedContext.Video> videos = new ArrayList<>(context.videos());
        videos.removeIf(video -> context.excluded().contains(video.contentId()));
        Collections.shuffle(videos, new Random(seed));
        ScoreBreakdown none = new ScoreBreakdown(0, 0, 0, 0, 0, List.of());
        return videos.stream()
                .limit(size)
                .map(video -> {
                    Candidate candidate = new Candidate(video, PoolType.FALLBACK, video.topicId());
                    return card(new Ranked(candidate, 0, none), List.of(), context);
                })
                .toList();
    }

    private List<Card> page(
            FeedContext context, Map<PoolType, Double> quotas, int size, Set<Integer> chosen, List<Card> before) {
        Map<PoolType, List<Ranked>> rankedByPool = new LinkedHashMap<>();
        Map<PoolType, Integer> available = new EnumMap<>(PoolType.class);
        for (PoolType type : quotas.keySet()) {
            CandidatePool pool = Objects.requireNonNull(pools.get(type), () -> "no pool for " + type);
            List<Ranked> ranked = pool.candidates(context).stream()
                    .filter(candidate -> !chosen.contains(candidate.video().contentId()))
                    .filter(candidate -> type == PoolType.REVIEW_DUE
                            || !context.completedRecently()
                                    .contains(candidate.video().contentId()))
                    .map(candidate -> ranker.rank(candidate, context))
                    .sorted(Comparator.comparingDouble(Ranked::score).reversed())
                    .toList();
            rankedByPool.put(type, ranked);
            available.put(type, ranked.size());
        }

        Map<PoolType, Integer> counts = QuotaAllocator.allocate(quotas, size, available);
        Set<Integer> onPage = new HashSet<>();
        List<Ranked> picked = new ArrayList<>();
        rankedByPool.forEach((type, ranked) -> ranked.stream()
                .filter(candidate -> onPage.add(candidate.candidate().video().contentId()))
                .limit(counts.getOrDefault(type, 0))
                .forEach(picked::add));
        picked.removeIf(
                ranked -> context.deferredTopics().contains(ranked.candidate().targetTopicId()));
        picked.sort(Comparator.comparingDouble(Ranked::score).reversed());
        return arrange(picked, before, context);
    }

    /** Rule 1, greedily: the best card that does not make a topic appear too often in a row. */
    private List<Card> arrange(List<Ranked> pending, List<Card> before, FeedContext context) {
        List<Card> arranged = new ArrayList<>();
        Map<Ranked, List<String>> notes = new LinkedHashMap<>();
        List<Integer> recentTopics = new ArrayList<>(before.stream()
                .map(card -> card.ranked().candidate().targetTopicId())
                .toList());
        List<Ranked> remaining = new ArrayList<>(pending);
        while (!remaining.isEmpty()) {
            Ranked next = remaining.stream()
                    .filter(candidate -> !wouldRepeatTooOften(recentTopics, candidate))
                    .findFirst()
                    .orElse(remaining.getFirst());
            for (Ranked skipped : remaining.subList(0, remaining.indexOf(next))) {
                notes.computeIfAbsent(skipped, key -> new ArrayList<>())
                        .add("same topic %d times in a row, moved down".formatted(maxSameTopicInRow + 1));
            }
            remaining.remove(next);
            recentTopics.add(next.candidate().targetTopicId());
            arranged.add(card(next, notes.getOrDefault(next, List.of()), context));
        }
        return arranged;
    }

    private boolean wouldRepeatTooOften(List<Integer> recentTopics, Ranked candidate) {
        if (recentTopics.size() < maxSameTopicInRow) {
            return false;
        }
        Integer topic = candidate.candidate().targetTopicId();
        return recentTopics.subList(recentTopics.size() - maxSameTopicInRow, recentTopics.size()).stream()
                .allMatch(topic::equals);
    }

    private Card card(Ranked ranked, List<String> appliedRules, FeedContext context) {
        ScoreBreakdown breakdown = ranked.breakdown();
        ScoreBreakdown withRules = new ScoreBreakdown(
                breakdown.weakness(),
                breakdown.examImportance(),
                breakdown.quality(),
                breakdown.freshness(),
                breakdown.difficultyFit(),
                appliedRules);
        ReasonTemplate template = Objects.requireNonNull(
                templates.get(ranked.candidate().pool()),
                () -> "no template for " + ranked.candidate().pool());
        return new Card(
                new Ranked(ranked.candidate(), ranked.score(), withRules),
                template.reason(ranked.candidate(), context));
    }

    /** A ranked candidate with its why-this-video text. */
    public record Card(Ranked ranked, String reasonText) {}
}
