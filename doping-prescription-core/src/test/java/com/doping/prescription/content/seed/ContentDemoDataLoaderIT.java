package com.doping.prescription.content.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.content.domain.AnswerOption;
import com.doping.prescription.content.domain.ContentKind;
import com.doping.prescription.content.domain.ContentStatus;
import com.doping.prescription.content.domain.MicroQuestionStatus;
import com.doping.prescription.content.domain.TagSource;
import com.doping.prescription.content.domain.VideoDetails;
import com.doping.prescription.content.persistence.ContentItem;
import com.doping.prescription.content.persistence.ContentItemRepository;
import com.doping.prescription.content.persistence.ContentTopic;
import com.doping.prescription.content.persistence.ContentTopicRepository;
import com.doping.prescription.content.persistence.MicroQuestion;
import com.doping.prescription.content.persistence.MicroQuestionRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class ContentDemoDataLoaderIT {

    private static final List<Integer> SEED_VIDEO_IDS =
            IntStream.rangeClosed(1, 40).boxed().toList();

    /** Untagged seed videos and the topic keyword tagging must find for each (docs/seed-verisi.md). */
    private static final Map<Integer, Integer> DRAFT_VIDEO_TOPICS = Map.of(8, 2, 20, 5, 28, 7, 40, 10);

    private static final Duration TAGGING_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private ContentDemoDataLoader loader;

    @Autowired
    private ContentItemRepository contents;

    @Autowired
    private ContentTopicRepository contentTopics;

    @Autowired
    private MicroQuestionRepository microQuestions;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void seedAndAwaitTagging() {
        loader.load();
        awaitDraftsTagged();
    }

    @Test
    void seedsTopicsKeywordsAndVideos() {
        assertThat(count("select count(*) from topic where id between 1 and 10"))
                .isEqualTo(10);
        assertThat(count("select count(*) from topic_keyword where topic_id between 1 and 10"))
                .isEqualTo(34);
        assertThat(seedVideos()).hasSize(40);
        assertThat(seedVideos())
                .filteredOn(video -> video.getStatus() == ContentStatus.LIVE)
                .hasSize(36)
                .allSatisfy(video -> assertThat(video.getPublishedAt()).isPresent());
    }

    @Test
    void seedsMicroQuestionsWithLetteredOptions() {
        assertThat(count("select count(*) from micro_question where id between 1 and 48"))
                .isEqualTo(48);
        MicroQuestion apiExample = microQuestions.findById(37).orElseThrow();
        assertThat(apiExample.getContentId()).isEqualTo(33);
        assertThat(apiExample.getStatus()).isEqualTo(MicroQuestionStatus.LIVE);
        assertThat(apiExample.getContent().options())
                .containsExactly(
                        new AnswerOption("A", "1/6"),
                        new AnswerOption("B", "1/3"),
                        new AnswerOption("C", "1/2"),
                        new AnswerOption("D", "2/3"));
        assertThat(apiExample.getContent().correctKey()).isEqualTo("B");
    }

    @Test
    void liveVideosAreManuallyTaggedAndApproved() {
        List<ContentTopic> manualLinks = seedLinks().stream()
                .filter(link -> link.getTagSource() == TagSource.MANUAL)
                .toList();

        assertThat(manualLinks).hasSize(36).allSatisfy(link -> {
            assertThat(link.getWeight()).isEqualByComparingTo("1.00");
            assertThat(link.getApprovedBy()).isPresent();
        });
    }

    @Test
    void untaggedVideosAreTaggedWithTheirOwnTopicOnly() {
        DRAFT_VIDEO_TOPICS.forEach((videoId, topicId) -> {
            assertThat(contents.findById(videoId).orElseThrow().getStatus()).isEqualTo(ContentStatus.TAGGED);
            assertThat(seedLinks())
                    .filteredOn(link -> link.getId().contentId().equals(videoId))
                    .singleElement()
                    .satisfies(link -> {
                        assertThat(link.getId().topicId()).isEqualTo(topicId);
                        assertThat(link.getTagSource()).isEqualTo(TagSource.KEYWORD_RULE);
                        assertThat(link.getApprovedBy()).isEmpty();
                    });
        });
    }

    @Test
    void rerunDoesNotDuplicateOrResetAnything() {
        // Given
        int keywordsBefore = count("select count(*) from topic_keyword");
        int linksBefore = seedLinks().size();

        // When
        loader.load();

        // Then
        assertThat(count("select count(*) from topic where id between 1 and 10"))
                .isEqualTo(10);
        assertThat(count("select count(*) from topic_keyword")).isEqualTo(keywordsBefore);
        assertThat(seedVideos()).hasSize(40);
        assertThat(seedLinks()).hasSize(linksBefore);
        assertThat(count("select count(*) from micro_question where id between 1 and 48"))
                .isEqualTo(48);
        DRAFT_VIDEO_TOPICS
                .keySet()
                .forEach(videoId -> assertThat(
                                contents.findById(videoId).orElseThrow().getStatus())
                        .isEqualTo(ContentStatus.TAGGED));
    }

    @Test
    void newVideoGetsIdAfterSeededIds() {
        // When
        ContentItem created = contents.saveAndFlush(new ContentItem(
                new VideoDetails(
                        "Yeni video", "", "Can Öğretmen", null, 40, 1, ContentKind.TIP, new BigDecimal("0.600")),
                Instant.parse("2026-10-20T07:00:00Z")));

        // Then
        assertThat(created.getId()).isGreaterThan(40);
        contents.delete(created);
    }

    private void awaitDraftsTagged() {
        await().atMost(TAGGING_TIMEOUT)
                .until(() -> DRAFT_VIDEO_TOPICS.keySet().stream()
                        .allMatch(id -> contents.findById(id).orElseThrow().getStatus() == ContentStatus.TAGGED));
    }

    private List<ContentItem> seedVideos() {
        return contents.findAllById(SEED_VIDEO_IDS);
    }

    private List<ContentTopic> seedLinks() {
        return contentTopics.findAll().stream()
                .filter(link -> SEED_VIDEO_IDS.contains(link.getId().contentId()))
                .toList();
    }

    private int count(String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }
}
