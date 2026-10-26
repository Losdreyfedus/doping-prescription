package com.doping.prescription.feed.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The bookmark on a Dopingle card and the Kaydedilen feed (docs/api.md, PUT/DELETE /api/feed/saved/:contentId). */
@IntegrationTest
@AutoConfigureMockMvc
class SavedContentIT {

    private static final String SLUG = "mert";
    private static final int OLASILIK_VIDEO = 34;
    private static final int TEMEL_VIDEO = 2;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private AuthDemoDataLoader users;

    @Autowired
    private ContentDemoDataLoader content;

    private String token;

    @BeforeEach
    void freshLearner() {
        users.load();
        content.load();
        UUID userId = jdbc.sql("select id from users where slug = :slug")
                .param("slug", SLUG)
                .query(UUID.class)
                .single();
        jdbc.sql("delete from saved_content where user_id = :userId")
                .param("userId", userId)
                .update();
        token = login();
    }

    @Test
    void savedVideosOpenAsTheirOwnFeedNewestFirst() {
        // Given: saved in this order, one of them twice
        assertThat(save(OLASILIK_VIDEO)).hasStatus(204);
        assertThat(save(TEMEL_VIDEO)).hasStatus(204);
        assertThat(save(OLASILIK_VIDEO)).hasStatus(204);

        // When
        JsonNode page = read(get("/api/feed?saved=true&refresh=true"));

        // Then
        assertThat(contentIds(page)).containsExactly(TEMEL_VIDEO, OLASILIK_VIDEO);
        assertThat(cards(page))
                .allSatisfy(card -> assertThat(card.get("saved").asBoolean()).isTrue());
    }

    @Test
    void aRemovedVideoLeavesTheSavedFeedAndItsCardIsNoLongerMarked() {
        // Given
        save(OLASILIK_VIDEO);

        // When
        MvcTestResult removed = mvc.delete()
                .uri("/api/feed/saved/" + OLASILIK_VIDEO)
                .header("Authorization", "Bearer " + token)
                .exchange();

        // Then
        assertThat(removed).hasStatus(204);
        assertThat(cards(read(get("/api/feed?saved=true&refresh=true")))).isEmpty();
        assertThat(cards(read(get("/api/feed?refresh=true&limit=20"))))
                .noneSatisfy(card -> assertThat(card.get("saved").asBoolean()).isTrue());
    }

    @Test
    void theGeneralFeedMarksTheCardsOfSavedVideos() {
        // Given
        JsonNode first = cards(read(get("/api/feed?refresh=true"))).getFirst();
        int contentId = first.at("/content/contentId").asInt();
        assertThat(first.get("saved").asBoolean()).isFalse();

        // When
        save(contentId);

        // Then
        assertThat(cards(read(get("/api/feed?refresh=true&limit=20"))))
                .filteredOn(card -> card.at("/content/contentId").asInt() == contentId)
                .allSatisfy(card -> assertThat(card.get("saved").asBoolean()).isTrue());
    }

    @Test
    void savingAnUnknownVideoIsNotFound() {
        MvcTestResult result = save(999_999);

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
    }

    private MvcTestResult save(int contentId) {
        return mvc.put()
                .uri("/api/feed/saved/" + contentId)
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    private MvcTestResult get(String uri) {
        MvcTestResult result =
                mvc.get().uri(uri).header("Authorization", "Bearer " + token).exchange();
        assertThat(result).hasStatusOk();
        return result;
    }

    private static List<JsonNode> cards(JsonNode page) {
        return StreamSupport.stream(page.get("cards").spliterator(), false).toList();
    }

    private static List<Integer> contentIds(JsonNode page) {
        return cards(page).stream()
                .map(card -> card.at("/content/contentId").asInt())
                .toList();
    }

    private String login() {
        return read(mvc.post()
                        .uri("/api/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + SLUG + "\"}")
                        .exchange())
                .get("token")
                .asString();
    }

    private JsonNode read(MvcTestResult result) {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }
}
