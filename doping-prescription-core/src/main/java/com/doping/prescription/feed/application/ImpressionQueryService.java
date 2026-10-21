package com.doping.prescription.feed.application;

import com.doping.prescription.feed.api.ImpressionQuery;
import com.doping.prescription.feed.domain.ScoreBreakdown;
import com.doping.prescription.feed.domain.ServedCard;
import com.doping.prescription.feed.persistence.FeedImpression;
import com.doping.prescription.feed.persistence.FeedImpressionRepository;
import com.doping.prescription.feed.persistence.FeedQueries;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves {@link ImpressionQuery} for the quiz and admin modules. */
@Service
class ImpressionQueryService implements ImpressionQuery {

    private final FeedImpressionRepository impressions;
    private final FeedQueries queries;

    ImpressionQueryService(FeedImpressionRepository impressions, FeedQueries queries) {
        this.impressions = impressions;
        this.queries = queries;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImpressionView> impression(UUID impressionId) {
        return impressions.findById(impressionId).map(impression -> {
            ServedCard card = impression.getCard();
            return new ImpressionView(
                    impression.getId(),
                    impression.getUserId(),
                    card.contentId(),
                    card.targetTopicId(),
                    card.pool().name(),
                    queries.maxWatchMs(impressionId));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<DebugCard> lastSession(UUID userId) {
        return impressions
                .findFirstByUserIdOrderByServedAtDesc(userId)
                .map(latest -> impressions.findBySessionIdOrderByPositionAsc(latest.getSessionId()).stream()
                        .map(ImpressionQueryService::debugCard)
                        .toList())
                .orElse(List.of());
    }

    private static DebugCard debugCard(FeedImpression impression) {
        ServedCard card = impression.getCard();
        ScoreBreakdown breakdown = card.breakdown();
        return new DebugCard(
                impression.getId(),
                card.position(),
                card.contentId(),
                card.targetTopicId(),
                card.pool().name(),
                card.score(),
                breakdown.weakness(),
                breakdown.examImportance(),
                breakdown.quality(),
                breakdown.freshness(),
                breakdown.difficultyFit(),
                breakdown.appliedRules(),
                card.reasonText());
    }
}
