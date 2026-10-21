package com.doping.prescription.content.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.content.domain.AnswerOption;
import com.doping.prescription.content.domain.ContentKind;
import com.doping.prescription.content.domain.ContentStatus;
import com.doping.prescription.content.domain.ExamType;
import com.doping.prescription.content.domain.MicroQuestionStatus;
import com.doping.prescription.content.domain.QuestionContent;
import com.doping.prescription.content.domain.TagSource;
import com.doping.prescription.content.domain.VideoDetails;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ContentPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-10-20T07:00:00Z");

    @Autowired
    private TopicRepository topics;

    @Autowired
    private TopicKeywordRepository keywords;

    @Autowired
    private ContentItemRepository contents;

    @Autowired
    private ContentTopicRepository contentTopics;

    @Autowired
    private MicroQuestionRepository microQuestions;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storesVideoWithTopicLinkKeywordAndMicroQuestion() {
        // Given
        Topic topic = topics.save(topic("TEST.OLAS"));
        keywords.save(new TopicKeyword(topic.getId(), "olasilik", new BigDecimal("1.00"), false));
        ContentItem video = contents.save(new ContentItem(video(3), NOW));
        ContentTopic link = new ContentTopic(
                new ContentTopicId(video.getId(), topic.getId()),
                new BigDecimal("1.00"),
                TagSource.MANUAL,
                new BigDecimal("1.00"),
                NOW);
        link.approve(UUID.randomUUID(), NOW);
        contentTopics.save(link);
        List<AnswerOption> options = List.of(
                new AnswerOption("A", "1/6"),
                new AnswerOption("B", "1/3"),
                new AnswerOption("C", "1/2"),
                new AnswerOption("D", "2/3"));
        MicroQuestion question = microQuestions.save(new MicroQuestion(
                video.getId(),
                topic.getId(),
                new QuestionContent("Bir zar atılıyor...", options, "B", "Çift sayılar 2, 4, 6.", 1),
                NOW));

        // When
        entityManager.flush();
        entityManager.clear();

        // Then
        ContentItem loadedVideo = contents.findById(video.getId()).orElseThrow();
        assertThat(loadedVideo.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(loadedVideo.getVideoUrl()).isEmpty();
        assertThat(contentTopics
                        .findById(new ContentTopicId(video.getId(), topic.getId()))
                        .orElseThrow()
                        .getApprovedBy())
                .isPresent();
        MicroQuestion loadedQuestion = microQuestions.findById(question.getId()).orElseThrow();
        assertThat(loadedQuestion.getContent().options()).containsExactlyElementsOf(options);
        assertThat(loadedQuestion.getContent().correctKey()).isEqualTo("B");
        assertThat(loadedQuestion.getStatus()).isEqualTo(MicroQuestionStatus.LIVE);
    }

    @Test
    void databaseRejectsDifficultyOutsideOneToFive() {
        // When / Then
        assertThatThrownBy(() -> contents.saveAndFlush(new ContentItem(video(6), NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsLinkToUnknownVideo() {
        // Given
        Topic topic = topics.save(topic("TEST.FK"));

        // When / Then
        assertThatThrownBy(() -> contentTopics.saveAndFlush(new ContentTopic(
                        new ContentTopicId(Integer.MAX_VALUE, topic.getId()),
                        BigDecimal.ONE,
                        TagSource.KEYWORD_RULE,
                        BigDecimal.ONE,
                        NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Topic topic(String code) {
        return new Topic(code, "Olasılık", ExamType.TYT, "Matematik", new BigDecimal("0.500"), 9, NOW);
    }

    private static VideoDetails video(int difficulty) {
        return new VideoDetails(
                "Olasılığın temeli",
                "İstenen bölü tüm durumlar",
                "Ayşe Öğretmen",
                null,
                58,
                difficulty,
                ContentKind.CONCEPT,
                new BigDecimal("0.900"));
    }
}
