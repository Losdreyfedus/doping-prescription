package com.doping.prescription.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.content.domain.ContentKind;
import com.doping.prescription.content.domain.ContentStatus;
import com.doping.prescription.content.domain.ExamType;
import com.doping.prescription.content.domain.TagSource;
import com.doping.prescription.content.domain.VideoDetails;
import com.doping.prescription.content.persistence.ContentItem;
import com.doping.prescription.content.persistence.ContentItemRepository;
import com.doping.prescription.content.persistence.ContentTopic;
import com.doping.prescription.content.persistence.ContentTopicRepository;
import com.doping.prescription.content.persistence.Topic;
import com.doping.prescription.content.persistence.TopicKeyword;
import com.doping.prescription.content.persistence.TopicKeywordRepository;
import com.doping.prescription.content.persistence.TopicRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** Publishes ContentCreated in a committed transaction and waits for the asynchronous tagging listener. */
@IntegrationTest
class ContentTaggingIT {

    private static final Instant NOW = Instant.parse("2026-10-20T07:00:00Z");
    private static final Duration LISTENER_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private TopicRepository topics;

    @Autowired
    private TopicKeywordRepository keywords;

    @Autowired
    private ContentItemRepository contents;

    @Autowired
    private ContentTopicRepository contentTopics;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcClient jdbc;

    private final List<Integer> createdVideos = new ArrayList<>();
    private Topic kumeler;
    private TopicKeyword kume;

    @BeforeEach
    void createDictionary() {
        kumeler = topics.save(new Topic(
                "TEST.KUME." + UUID.randomUUID().toString().substring(0, 8),
                "Kümeler",
                ExamType.TYT,
                "Matematik",
                new BigDecimal("0.400"),
                7,
                NOW));
        kume = keywords.save(new TopicKeyword(kumeler.getId(), "kume", new BigDecimal("1.00"), false));
    }

    @AfterEach
    void removeCreatedRows() {
        contentTopics.deleteAll(contentTopics.findAll().stream()
                .filter(link -> createdVideos.contains(link.getId().contentId()))
                .toList());
        contents.deleteAllById(createdVideos);
        keywords.delete(kume);
        topics.delete(kumeler);
    }

    @Test
    void tagsMatchingDraftAndAwaitsApproval() {
        // When
        Integer videoId = createVideo("Kümelerde kesişim ve birleşim");

        // Then
        await().atMost(LISTENER_TIMEOUT)
                .until(() -> contents.findById(videoId).orElseThrow().getStatus() == ContentStatus.TAGGED);
        // Seed topics in the shared database may match too (e.g. the seeded Kümeler topic); check our own topic.
        assertThat(linksOf(videoId))
                .filteredOn(link -> link.getId().topicId().equals(kumeler.getId()))
                .singleElement()
                .satisfies(link -> {
                    assertThat(link.getId().topicId()).isEqualTo(kumeler.getId());
                    assertThat(link.getTagSource()).isEqualTo(TagSource.KEYWORD_RULE);
                    assertThat(link.getWeight()).isEqualByComparingTo("1.00");
                    assertThat(link.getConfidence()).isEqualByComparingTo("1.00");
                    assertThat(link.getApprovedBy()).isEmpty();
                });
    }

    @Test
    void videoWithoutMatchingKeywordStaysDraft() {
        // When
        Integer videoId = createVideo("Sayı doğrusunda sıralama");
        awaitAllEventsHandled();

        // Then
        assertThat(contents.findById(videoId).orElseThrow().getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(linksOf(videoId)).isEmpty();
    }

    @Test
    void replayedEventDoesNotTagTwice() {
        // Given
        Integer videoId = createVideo("Kümeler, 3 örnek");
        await().atMost(LISTENER_TIMEOUT)
                .until(() -> contents.findById(videoId).orElseThrow().getStatus() == ContentStatus.TAGGED);
        List<ContentTopic> linksBeforeReplay = linksOf(videoId);

        // When
        transaction.executeWithoutResult(status -> events.publishEvent(new ContentCreated(videoId)));
        awaitAllEventsHandled();

        // Then
        assertThat(linksOf(videoId)).hasSameSizeAs(linksBeforeReplay);
    }

    private Integer createVideo(String title) {
        Integer videoId = transaction.execute(status -> {
            ContentItem video = contents.save(new ContentItem(
                    new VideoDetails(
                            title, "", "Can Öğretmen", null, 60, 2, ContentKind.CONCEPT, new BigDecimal("0.700")),
                    NOW));
            events.publishEvent(new ContentCreated(video.getId()));
            return video.getId();
        });
        createdVideos.add(videoId);
        return videoId;
    }

    private List<ContentTopic> linksOf(Integer videoId) {
        return contentTopics.findAll().stream()
                .filter(link -> link.getId().contentId().equals(videoId))
                .toList();
    }

    private void awaitAllEventsHandled() {
        await().atMost(LISTENER_TIMEOUT)
                .until(() -> jdbc.sql("select count(*) from event_publication where completion_date is null")
                                .query(Integer.class)
                                .single()
                        == 0);
    }
}
