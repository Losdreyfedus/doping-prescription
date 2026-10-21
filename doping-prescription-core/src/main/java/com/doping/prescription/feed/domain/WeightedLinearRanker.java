package com.doping.prescription.feed.domain;

import com.doping.prescription.feed.domain.FeedContext.TopicState;
import java.util.List;
import java.util.Objects;

/** The linear score of docs/algoritma.md §3. */
public final class WeightedLinearRanker implements FeedRanker {

    static final String VERSION = "v1";
    private static final double MIN_CONFIDENCE = 0.3;
    private static final double WATCHED_FRESHNESS = 0.2;
    private static final double MAX_DIFFICULTY_STEPS = 4;

    private final RankWeights weights;

    public WeightedLinearRanker(RankWeights weights) {
        this.weights = Objects.requireNonNull(weights, "weights");
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public Ranked rank(Candidate candidate, FeedContext context) {
        TopicState topic = context.topic(candidate.targetTopicId());
        boolean addressesWeakness = candidate.pool() == PoolType.WEAK_TOPIC || candidate.pool() == PoolType.REVIEW_DUE;
        double weakness = addressesWeakness ? (1 - topic.mastery()) * Math.max(topic.confidence(), MIN_CONFIDENCE) : 0;
        double freshness = context.watchedEver().contains(candidate.video().contentId()) ? WATCHED_FRESHNESS : 1;
        double difficultyFit =
                1 - Math.abs((candidate.video().difficulty() - 1) / MAX_DIFFICULTY_STEPS - topic.mastery());
        ScoreBreakdown breakdown = new ScoreBreakdown(
                weakness, topic.examWeight(), candidate.video().quality(), freshness, difficultyFit, List.of());
        double examImportanceWeight = context.examNear() ? weights.examNearImportance() : weights.examImportance();
        double score = weights.weakness() * weakness
                + examImportanceWeight * topic.examWeight()
                + weights.quality() * candidate.video().quality()
                + weights.freshness() * freshness
                + weights.difficultyFit() * difficultyFit;
        return new Ranked(candidate, score, breakdown);
    }

    /**
     * @param examNearImportance examImportance weight when the exam date is close
     */
    public record RankWeights(
            double weakness,
            double examImportance,
            double quality,
            double freshness,
            double difficultyFit,
            double examNearImportance) {}
}
