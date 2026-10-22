package com.doping.prescription.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.admin.application.ContentEffectivenessService.VideoEffectiveness;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.api.MicroQuestionCatalog;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Video effectiveness: answers of each video's micro-questions, best correct-answer rate first. */
@IntegrationTest
class ContentEffectivenessIT {

    /** Olasılık's first video has questions 37 and 38 (docs/seed-verisi.md); question 1 belongs to video 1. */
    private static final int OLASILIK_FIRST = 37;

    private static final int OLASILIK_SECOND = 38;
    private static final int FIRST_VIDEO_QUESTION = 1;

    @Autowired
    private ContentEffectivenessService effectiveness;

    @Autowired
    private MicroQuestionCatalog questions;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private AuthDemoDataLoader users;

    @Autowired
    private ContentDemoDataLoader content;

    @BeforeEach
    void seed() {
        users.load();
        content.load();
        // answers of other tests in this shared database would change the numbers
        jdbc.sql("delete from micro_quiz_attempt").update();
    }

    @Test
    void sumsAVideosQuestionsAndSortsByCorrectRate() {
        answer(OLASILIK_FIRST, true);
        answer(OLASILIK_SECOND, false);
        answer(OLASILIK_SECOND, false);
        answer(FIRST_VIDEO_QUESTION, true);

        List<VideoEffectiveness> videos = effectiveness.byVideo();

        Integer olasilikVideo = questions.question(OLASILIK_FIRST).orElseThrow().contentId();
        assertThat(videos).hasSize(2);
        assertThat(videos.getFirst().correctRate()).isEqualTo(1.0);
        assertThat(videos.getLast()).satisfies(video -> {
            assertThat(video.contentId()).isEqualTo(olasilikVideo);
            assertThat(video.topic()).isEqualTo("Olasılık");
            assertThat(video.answers()).isEqualTo(3);
            assertThat(video.correct()).isEqualTo(1);
            assertThat(video.correctRate()).isCloseTo(1.0 / 3, within(1e-9));
        });
    }

    private void answer(int questionId, boolean correct) {
        jdbc.sql("""
                        insert into micro_quiz_attempt
                            (id, user_id, micro_question_id, impression_id, selected_key, is_correct, response_ms, points_awarded)
                        values (:id, :userId, :question, :impression, 'A', :correct, 5000, 0)
                        """)
                .param("id", UUID.randomUUID())
                .param("userId", UUID.randomUUID())
                .param("question", questionId)
                .param("impression", UUID.randomUUID())
                .param("correct", correct)
                .update();
    }
}
