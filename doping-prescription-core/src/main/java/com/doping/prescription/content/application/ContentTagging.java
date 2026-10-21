package com.doping.prescription.content.application;

import com.doping.prescription.content.domain.ContentStatus;
import com.doping.prescription.content.domain.ContentTagger;
import com.doping.prescription.content.domain.TagSource;
import com.doping.prescription.content.domain.TagSuggestion;
import com.doping.prescription.content.domain.TaggingKeyword;
import com.doping.prescription.content.persistence.ContentItem;
import com.doping.prescription.content.persistence.ContentItemRepository;
import com.doping.prescription.content.persistence.ContentTopic;
import com.doping.prescription.content.persistence.ContentTopicId;
import com.doping.prescription.content.persistence.ContentTopicRepository;
import com.doping.prescription.content.persistence.TopicKeywordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * Tags a newly created video with topic suggestions from the keyword dictionary (docs/algoritma.md §5). Runs after the
 * creating transaction commits, in its own transaction; the event publication registry retries it if it fails.
 */
@Service
class ContentTagging {

    private static final Logger log = LoggerFactory.getLogger(ContentTagging.class);

    /** content_topic_map.weight and confidence are numeric(3,2). */
    private static final int STORED_SCALE = 2;

    private final ContentItemRepository contents;
    private final ContentTopicRepository contentTopics;
    private final TopicKeywordRepository keywords;
    private final ContentTagger tagger;
    private final Clock clock;

    ContentTagging(
            ContentItemRepository contents,
            ContentTopicRepository contentTopics,
            TopicKeywordRepository keywords,
            ContentTagger tagger,
            Clock clock) {
        this.contents = contents;
        this.contentTopics = contentTopics;
        this.keywords = keywords;
        this.tagger = tagger;
        this.clock = clock;
    }

    @ApplicationModuleListener
    public void on(ContentCreated event) {
        ContentItem video = contents.findById(event.contentId())
                .orElseThrow(() -> new IllegalStateException("Unknown video " + event.contentId()));
        // Idempotent: a replayed event finds the video already tagged (or approved) and does nothing.
        if (video.getStatus() != ContentStatus.DRAFT) {
            return;
        }
        List<TagSuggestion> suggestions = tagger.suggest(video.getTitle(), video.getDescription(), dictionary());
        if (suggestions.isEmpty()) {
            log.info("No topic suggestion for video contentId={}; it stays DRAFT", video.getId());
            return;
        }
        Instant now = clock.instant();
        suggestions.forEach(suggestion -> contentTopics.save(link(video.getId(), suggestion, now)));
        video.markTagged(now);
        log.info("Tagged video contentId={} with {} topic suggestion(s)", video.getId(), suggestions.size());
    }

    private List<TaggingKeyword> dictionary() {
        return keywords.findAll().stream()
                .map(keyword -> new TaggingKeyword(
                        keyword.getTopicId(),
                        keyword.getKeyword(),
                        keyword.getWeight().doubleValue(),
                        keyword.isNegative()))
                .toList();
    }

    private static ContentTopic link(Integer contentId, TagSuggestion suggestion, Instant now) {
        BigDecimal score = BigDecimal.valueOf(suggestion.score()).setScale(STORED_SCALE, RoundingMode.HALF_UP);
        return new ContentTopic(
                new ContentTopicId(contentId, suggestion.topicId()), score, TagSource.KEYWORD_RULE, score, now);
    }
}
