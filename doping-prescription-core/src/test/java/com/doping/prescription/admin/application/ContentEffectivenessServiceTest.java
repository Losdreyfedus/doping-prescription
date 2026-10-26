package com.doping.prescription.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.doping.prescription.admin.application.ContentEffectivenessService.VideoEffectiveness;
import com.doping.prescription.content.api.ContentCatalog;
import com.doping.prescription.content.api.ContentCatalog.VideoView;
import com.doping.prescription.content.api.MicroQuestionCatalog;
import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.content.api.TopicCatalog.TopicView;
import com.doping.prescription.quiz.api.QuizStatistics;
import com.doping.prescription.quiz.api.QuizStatistics.QuestionStats;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The other modules' APIs are mocked: this checks the admin module's own arithmetic and labelling. */
class ContentEffectivenessServiceTest {

    private static final int OLASILIK = 9;
    private static final int MISSING_TOPIC = 99;

    private final QuizStatistics quizzes = mock(QuizStatistics.class);
    private final MicroQuestionCatalog questions = mock(MicroQuestionCatalog.class);
    private final ContentCatalog content = mock(ContentCatalog.class);
    private final TopicCatalog topics = mock(TopicCatalog.class);
    private final ContentEffectivenessService service =
            new ContentEffectivenessService(quizzes, questions, content, topics);

    @Test
    void sumsTheQuestionsOfAVideoAndLabelsAnUnknownTopic() {
        // Given: video 1 has two answered questions, video 2 belongs to a topic the catalog does not know
        when(quizzes.answersByQuestion())
                .thenReturn(
                        List.of(new QuestionStats(11, 2, 1), new QuestionStats(12, 2, 2), new QuestionStats(21, 4, 1)));
        when(questions.contentIdsOf(anyCollection())).thenReturn(Map.of(11, 1, 12, 1, 21, 2));
        when(content.videos(any())).thenReturn(List.of(video(1, OLASILIK), video(2, MISSING_TOPIC)));
        when(topics.topicsById()).thenReturn(Map.of(OLASILIK, new TopicView(OLASILIK, "OLASILIK", "Olasılık", 1.0)));

        // When
        List<VideoEffectiveness> videos = service.byVideo();

        // Then
        assertThat(videos)
                .containsExactly(
                        new VideoEffectiveness(1, "Video 1", "Olasılık", 4, 3, 0.75),
                        new VideoEffectiveness(2, "Video 2", ContentEffectivenessService.UNKNOWN_TOPIC, 4, 1, 0.25));
    }

    @Test
    void aVideoWithoutAnswersHasRateZero() {
        assertThat(new ContentEffectivenessService.Tally(0, 0).correctRate()).isZero();
    }

    private static VideoView video(int contentId, int topicId) {
        return new VideoView(contentId, "Video " + contentId, "Öğretmen", null, 60, "LECTURE", 2, 0.8, topicId, true);
    }
}
