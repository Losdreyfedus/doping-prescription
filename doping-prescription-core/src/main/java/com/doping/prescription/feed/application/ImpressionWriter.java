package com.doping.prescription.feed.application;

import com.doping.prescription.feed.domain.ServedCard;
import com.doping.prescription.feed.persistence.FeedImpression;
import com.doping.prescription.feed.persistence.FeedImpressionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Records the cards of a page as impressions, numbered from the cursor's position. */
@Component
class ImpressionWriter {

    private final FeedImpressionRepository impressions;
    private final Clock clock;

    ImpressionWriter(FeedImpressionRepository impressions, Clock clock) {
        this.impressions = impressions;
        this.clock = clock;
    }

    /** All impressions of a page in one transaction; public like every transaction boundary (kod-standartlari §2). */
    @Transactional
    public List<Served> write(UUID userId, FeedCursor cursor, List<ServedCard> cards, String algoVersion) {
        Instant now = clock.instant();
        List<Served> served = new ArrayList<>();
        for (int index = 0; index < cards.size(); index++) {
            ServedCard card = cards.get(index);
            ServedCard positioned = new ServedCard(
                    card.contentId(),
                    cursor.position() + index,
                    card.pool(),
                    card.targetTopicId(),
                    card.score(),
                    card.breakdown(),
                    card.reasonText());
            FeedImpression impression =
                    new FeedImpression(UUID.randomUUID(), userId, cursor.sessionId(), positioned, algoVersion, now);
            impressions.save(impression);
            served.add(new Served(impression.getId(), positioned));
        }
        return served;
    }

    /** A card with the impression it was recorded as. */
    record Served(UUID impressionId, ServedCard card) {}
}
