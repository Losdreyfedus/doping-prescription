package com.doping.prescription.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.feed.application.FeedViews.FeedPage;
import com.doping.prescription.learner.api.LearnerProfileQuery;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** The feed never answers with an error (AGENTS.md rule 7): a failing profile read falls back to the general feed. */
@IntegrationTest
class FeedFallbackIT {

    // A spy keeps the real bean (and its type for the learner module's own collaborators) and only fails this call
    @MockitoSpyBean
    private LearnerProfileQuery profiles;

    @Autowired
    private FeedService feed;

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
    }

    @Test
    void aFailingProfileServesTheFallbackFeedInsteadOfAnError() {
        // Given
        doThrow(new DataAccessResourceFailureException("database down"))
                .when(profiles)
                .profile(any(UUID.class));

        // When
        FeedPage page = feed.page(mertId(), null, 10, null, false);

        // Then
        assertThat(page.personalized()).isFalse();
        assertThat(page.cards())
                .hasSize(10)
                .allSatisfy(card -> assertThat(card.pool()).isEqualTo("FALLBACK"));
    }

    private UUID mertId() {
        return jdbc.sql("select id from users where slug = 'mert'")
                .query(UUID.class)
                .single();
    }
}
