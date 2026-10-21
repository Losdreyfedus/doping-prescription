package com.doping.prescription.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@AutoConfigureMockMvc
class AuthIT {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AuthDemoDataLoader users;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void seedUsers() {
        users.load();
    }

    @Test
    void listsDemoUsersStudentsFirstWithoutToken() {
        MvcTestResult result = mvc.get().uri("/api/auth/demo-users").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[*].slug").asArray().startsWith("deniz", "mert", "selin");
        assertThat(result).bodyJson().extractingPath("$[-1].role").isEqualTo("ADMIN");
    }

    @Test
    void loginReturnsTokenThatIdentifiesTheUser() {
        String token = login("deniz");

        MvcTestResult me = mvc.get()
                .uri("/api/me")
                .header("Authorization", "Bearer " + token)
                .exchange();

        assertThat(me).hasStatusOk();
        assertThat(me).bodyJson().extractingPath("$.slug").isEqualTo("deniz");
        assertThat(me).bodyJson().extractingPath("$.role").isEqualTo("STUDENT");
        assertThat(me).bodyJson().extractingPath("$.userId").isEqualTo("00000000-0000-4000-8000-000000000001");
    }

    @Test
    void unknownUserCannotLogIn() {
        MvcTestResult result = loginRequest("nobody");

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void blankSlugIsInvalidRequest() {
        MvcTestResult result = loginRequest("");

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    void protectedEndpointWithoutTokenIsUnauthorizedProblem() {
        MvcTestResult result = mvc.get().uri("/api/me").exchange();

        assertThat(result).hasStatus(401);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = login("deniz");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        MvcTestResult result = mvc.get()
                .uri("/api/me")
                .header("Authorization", "Bearer " + tampered)
                .exchange();

        assertThat(result).hasStatus(401);
    }

    @Test
    void adminEndpointsAreForbiddenForStudents() {
        MvcTestResult result = mvc.get()
                .uri("/api/admin/users")
                .header("Authorization", "Bearer " + login("deniz"))
                .exchange();

        assertThat(result).hasStatus(403);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
    }

    @Test
    void adminCanListUsers() {
        MvcTestResult result = mvc.get()
                .uri("/api/admin/users")
                .header("Authorization", "Bearer " + login("admin"))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$[?(@.slug == 'selin')].personalizationEnabled")
                .asArray()
                .containsExactly(false);
    }

    private String login(String slug) {
        MvcTestResult result = loginRequest(slug);
        assertThat(result).hasStatusOk();
        return json.readTree(result.getResponse().getContentAsByteArray())
                .get("token")
                .asString();
    }

    private MvcTestResult loginRequest(String slug) {
        return mvc.post()
                .uri("/api/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"" + slug + "\"}")
                .exchange();
    }
}
