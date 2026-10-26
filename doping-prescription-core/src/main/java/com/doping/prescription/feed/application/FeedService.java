package com.doping.prescription.feed.application;

import com.doping.prescription.auth.api.UserDirectory;
import com.doping.prescription.auth.api.UserDirectory.UserSummary;
import com.doping.prescription.content.api.ContentCatalog;
import com.doping.prescription.content.api.ContentCatalog.VideoView;
import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.content.api.TopicCatalog.TopicView;
import com.doping.prescription.feed.application.FeedViews.CardView;
import com.doping.prescription.feed.application.FeedViews.FeedPage;
import com.doping.prescription.feed.domain.FeedComposer;
import com.doping.prescription.feed.domain.FeedComposer.Card;
import com.doping.prescription.feed.domain.FeedContext;
import com.doping.prescription.feed.domain.PoolType;
import com.doping.prescription.feed.domain.ServedCard;
import com.doping.prescription.feed.persistence.SavedContentQueries;
import com.doping.prescription.shared.error.DomainException;
import com.doping.prescription.shared.error.ErrorCode;
import com.doping.prescription.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serves feed pages (docs/api.md, GET /api/feed): a prepared Redis queue per learner, rebuilt when a session starts or
 * runs low, and a fallback feed whenever personalization fails, so the feed never answers with an error (AGENTS.md
 * rule 7).
 */
@Service
public class FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
    private static final int SCORE_SCALE = 4;

    private final FeedComposer composer;
    private final FeedContextLoader loader;
    private final FeedStore store;
    private final ImpressionWriter impressions;
    private final FeedViews views;
    private final UserDirectory users;
    private final ContentCatalog content;
    private final TopicCatalog topics;
    private final FeedProperties properties;
    private final SavedContentQueries saved;

    @SuppressWarnings("checkstyle:ParameterNumber") // the feed coordinates ten collaborators; each is one concern
    FeedService(
            FeedComposer composer,
            FeedContextLoader loader,
            FeedStore store,
            ImpressionWriter impressions,
            FeedViews views,
            UserDirectory users,
            ContentCatalog content,
            TopicCatalog topics,
            FeedProperties properties,
            SavedContentQueries saved) {
        this.composer = composer;
        this.loader = loader;
        this.store = store;
        this.impressions = impressions;
        this.views = views;
        this.users = users;
        this.content = content;
        this.topics = topics;
        this.properties = properties;
        this.saved = saved;
    }

    /**
     * @param cursorText     {@code nextCursor} of the previous page; none starts a new session
     * @param requestedLimit cards on this page; none means {@code prescription.feed.page-size}, more than
     *                       {@code page-size-max} is rejected
     * @param topicId        only this topic's videos (Gelişimim → Çalış)
     * @param refresh        start a new session and rebuild the queue
     */
    public FeedPage page(
            UUID userId,
            @Nullable String cursorText,
            @Nullable Integer requestedLimit,
            @Nullable Integer topicId,
            boolean refresh) {
        return page(userId, cursorText, requestedLimit, topicId, refresh, false);
    }

    /**
     * @param savedOnly only the learner's saved videos, newest saved first (Kaydedilen); takes precedence over
     *     {@code topicId}
     */
    @SuppressWarnings("checkstyle:IllegalCatch") // any failure of personalization falls back (AGENTS.md rule 7)
    public FeedPage page(
            UUID userId,
            @Nullable String cursorText,
            @Nullable Integer requestedLimit,
            @Nullable Integer topicId,
            boolean refresh,
            boolean savedOnly) {
        int limit = pageLimit(requestedLimit, properties);
        boolean newSession = refresh || cursorText == null;
        FeedCursor cursor = newSession ? FeedCursor.start() : FeedCursor.decode(cursorText);
        UserSummary user = users.findById(userId).orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));
        List<VideoView> videos = content.liveVideos();
        Map<Integer, TopicView> catalog = topics.topicsById();
        if (topicId != null && !catalog.containsKey(topicId)) {
            throw new NotFoundException("Konu bulunamadı.");
        }

        Selection selection;
        try {
            FeedContext context = loader.load(user, cursor.sessionId(), videos, catalog);
            selection = savedOnly
                    ? new Selection(served(composer.chosen(context, saved.newestFirst(userId), limit)), true)
                    : select(user, context, cursor, limit, topicId, newSession);
        } catch (RuntimeException e) {
            log.warn("Personalized feed failed for session {}, serving the fallback feed", cursor.sessionId(), e);
            selection = fallback(userId, cursor, videos, limit);
        }

        Map<Integer, VideoView> videosById =
                videos.stream().collect(Collectors.toMap(VideoView::contentId, Function.identity()));
        // a queued card whose video was retired meanwhile is dropped
        List<ServedCard> live = selection.cards().stream()
                .filter(card -> videosById.containsKey(card.contentId()))
                .toList();
        List<CardView> cards = cardViews(userId, cursor, live, videosById, catalog);
        store.markSeen(
                userId,
                cursor.sessionId(),
                live.stream().map(ServedCard::contentId).toList());
        return new FeedPage(
                cursor.sessionId(),
                selection.personalized(),
                composer.algoVersion(),
                cursor.advance(cards.size()).encode(),
                properties.demoVideoSpeed(),
                cards);
    }

    private Selection select(
            UserSummary user,
            FeedContext context,
            FeedCursor cursor,
            int limit,
            @Nullable Integer topicId,
            boolean newSession) {
        if (!user.personalizationEnabled()) {
            return new Selection(served(composer.fallback(context, sessionRandom(cursor), limit)), false);
        }
        if (topicId != null) {
            return new Selection(served(composer.topicOnly(context, topicId, limit)), true);
        }
        boolean personalized = context.hasSignals();
        Map<PoolType, Double> quotas = personalized
                ? properties.quota().shares()
                : properties.coldStartQuota().shares();
        Optional<List<ServedCard>> stored = store.queue(user.userId());
        if (stored.isEmpty()) {
            // Redis unavailable: compute the pages on every request; the session's seen videos come from impressions
            return new Selection(take(compose(context, quotas, personalized), limit), personalized);
        }
        List<ServedCard> queue = new ArrayList<>(newSession ? List.of() : unseen(stored.get(), context.excluded()));
        if (queue.size() < Math.max(limit, properties.queueRefillThreshold())) {
            queue = new ArrayList<>(compose(context, quotas, personalized));
        }
        List<ServedCard> page = take(queue, limit);
        store.replaceQueue(user.userId(), queue.subList(page.size(), queue.size()));
        return new Selection(page, personalized);
    }

    private Selection fallback(UUID userId, FeedCursor cursor, List<VideoView> videos, int limit) {
        FeedContext context = loader.fallbackContext(userId, cursor.sessionId(), videos);
        return new Selection(served(composer.fallback(context, sessionRandom(cursor), limit)), false);
    }

    /** Records the cards as impressions and renders them, marking the videos the learner saved. */
    private List<CardView> cardViews(
            UUID userId,
            FeedCursor cursor,
            List<ServedCard> live,
            Map<Integer, VideoView> videosById,
            Map<Integer, TopicView> catalog) {
        Set<Integer> savedIds = saved.savedAmong(
                userId, live.stream().map(ServedCard::contentId).toList());
        return impressions.write(userId, cursor, live, composer.algoVersion()).stream()
                .map(served -> views.card(
                        served,
                        videosById,
                        catalog,
                        savedIds.contains(served.card().contentId())))
                .toList();
    }

    /** The page size: the configured default if none was asked for, else within 1..page-size-max (docs/api.md). */
    static int pageLimit(@Nullable Integer requested, FeedProperties properties) {
        if (requested == null) {
            return properties.pageSize();
        }
        if (requested < 1 || requested > properties.pageSizeMax()) {
            throw new InvalidLimitException(properties.pageSizeMax());
        }
        return requested;
    }

    /** Pinned videos open only a personalized feed: before the first exam the feed must stay general. */
    private List<ServedCard> compose(FeedContext context, Map<PoolType, Double> quotas, boolean personalized) {
        return served(composer.compose(
                context,
                quotas,
                properties.pageSize(),
                properties.queuePages(),
                personalized ? properties.pinnedContentIds() : List.of()));
    }

    private static List<ServedCard> unseen(List<ServedCard> queue, Set<Integer> seen) {
        return queue.stream().filter(card -> !seen.contains(card.contentId())).toList();
    }

    private static List<ServedCard> take(List<ServedCard> cards, int limit) {
        return List.copyOf(cards.subList(0, Math.min(limit, cards.size())));
    }

    private static List<ServedCard> served(List<Card> cards) {
        return cards.stream()
                .map(card -> new ServedCard(
                        card.ranked().candidate().video().contentId(),
                        0,
                        card.ranked().candidate().pool(),
                        card.ranked().candidate().targetTopicId(),
                        BigDecimal.valueOf(card.ranked().score()).setScale(SCORE_SCALE, RoundingMode.HALF_UP),
                        card.ranked().breakdown(),
                        card.reasonText()))
                .toList();
    }

    /** The same session always gets the same fallback order (docs/algoritma.md §2, seeded by sessionId). */
    private static RandomGenerator sessionRandom(FeedCursor cursor) {
        return new Random(
                cursor.sessionId().getMostSignificantBits() ^ cursor.sessionId().getLeastSignificantBits());
    }

    private record Selection(List<ServedCard> cards, boolean personalized) {}

    /** A page size outside 1..page-size-max (docs/api.md: 400). */
    static final class InvalidLimitException extends DomainException {

        private static final long serialVersionUID = 1L;

        InvalidLimitException(int max) {
            super(ErrorCode.INVALID_REQUEST, "Sayfa boyutu 1 ile %d arasında olmalı.".formatted(max));
        }
    }
}
