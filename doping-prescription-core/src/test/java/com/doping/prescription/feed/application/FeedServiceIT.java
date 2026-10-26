package com.doping.prescription.feed.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import com.doping.prescription.feed.application.FeedViews.CardView;
import com.doping.prescription.feed.application.FeedViews.FeedPage;
import com.doping.prescription.feed.application.InteractionService.Interaction;
import com.doping.prescription.feed.domain.InteractionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

/**
 * Feed behaviour without Redis or Kafka: this context points both at unreachable ports, so every page here also
 * proves that the feed still answers when Redis is down (AGENTS.md §7, phase 5).
 */
@IntegrationTest
@AutoConfigureMockMvc
class FeedServiceIT {

    @Autowired
    private FeedService feed;

    @Autowired
    private InteractionService interactions;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper json;

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
    void withoutRedisTheFeedStillAnswersAndKeepsTheSessionFreeOfRepeats() {
        UUID mert = userId("mert");

        FeedPage first = feed.page(mert, null, 10, null, false);
        FeedPage second = feed.page(mert, first.nextCursor(), 10, null, false);

        assertThat(first.cards()).hasSize(10);
        assertThat(second.sessionId()).isEqualTo(first.sessionId());
        assertThat(contentIds(second)).doesNotContainAnyElementsOf(contentIds(first));
    }

    @Test
    void aStudentWithPersonalizationOffGetsTheFallbackFeed() {
        FeedPage page = feed.page(userId("selin"), null, 10, null, false);

        assertThat(page.personalized()).isFalse();
        assertThat(page.cards()).hasSize(10).allSatisfy(card -> {
            assertThat(card.pool()).isEqualTo("FALLBACK");
            assertThat(card.reasonText()).isEqualTo("Dopingle'da yeni");
            assertThat(card.pointsAvailable()).isEqualTo(10);
        });
    }

    @Test
    void anInteractionIsStoredOnceAndOnlyForTheCardsOwner() {
        UUID mert = userId("mert");
        CardView card = feed.page(mert, null, 1, null, true).cards().getFirst();
        Interaction complete = new Interaction(
                "evt-" + UUID.randomUUID(), card.impressionId(), InteractionType.VIEW_COMPLETE, 41_000, Instant.now());

        interactions.record(mert, List.of(complete, complete));
        interactions.record(
                userId("deniz"),
                List.of(new Interaction(
                        "evt-" + UUID.randomUUID(), card.impressionId(), InteractionType.LIKE, null, Instant.now())));

        assertThat(jdbc.sql("select count(*) from feed_interaction where impression_id = :id")
                        .param("id", card.impressionId())
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void withoutALimitThePageHasTheConfiguredPageSize() {
        assertThat(mvc.get()
                        .uri("/api/feed")
                        .header("Authorization", "Bearer " + jwt("mert"))
                        .exchange())
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.cards.length()")
                .isEqualTo(10);
    }

    @Test
    void invalidRequestsAreRejected() {
        String bearer = "Bearer " + jwt("mert");

        assertThat(mvc.get()
                        .uri("/api/feed?cursor=not-a-cursor")
                        .header("Authorization", bearer)
                        .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("INVALID_REQUEST");
        assertThat(mvc.get()
                        .uri("/api/feed?limit=21")
                        .header("Authorization", bearer)
                        .exchange())
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("INVALID_REQUEST");
        assertThat(mvc.get()
                        .uri("/api/feed?limit=0")
                        .header("Authorization", bearer)
                        .exchange())
                .hasStatus(400);
        assertThat(mvc.get()
                        .uri("/api/feed?topicId=99")
                        .header("Authorization", bearer)
                        .exchange())
                .hasStatus(404);
        assertThat(mvc.get().uri("/api/feed").exchange()).hasStatus(401);
    }

    private String jwt(String slug) {
        return json.readTree(mvc.post()
                        .uri("/api/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\"}")
                        .exchange()
                        .getResponse()
                        .getContentAsByteArray())
                .get("token")
                .asString();
    }

    private UUID userId(String slug) {
        return jdbc.sql("select id from users where slug = :slug")
                .param("slug", slug)
                .query(UUID.class)
                .single();
    }

    private static List<Integer> contentIds(FeedPage page) {
        return page.cards().stream().map(card -> card.content().contentId()).toList();
    }
}
