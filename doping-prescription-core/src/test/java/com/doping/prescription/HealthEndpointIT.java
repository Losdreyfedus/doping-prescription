package com.doping.prescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.doping.prescription.auth.seed.AuthDemoDataLoader;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = IntegrationTest.JWT_SECRET_PROPERTY)
@AutoConfigureMockMvc
@Import(InfrastructureContainersConfiguration.class)
class HealthEndpointIT {

    // A freshly started Kafka container may need a few seconds before it answers metadata requests.
    private static final Duration INFRASTRUCTURE_READY_TIMEOUT = Duration.ofSeconds(30);

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
    void adminSeesPostgresRedisAndKafkaAsUp() {
        String adminToken = login("admin");

        await().atMost(INFRASTRUCTURE_READY_TIMEOUT).untilAsserted(() -> {
            MvcTestResult result = health(adminToken);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("UP");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.components.db.status")
                    .isEqualTo("UP");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.components.redis.status")
                    .isEqualTo("UP");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.components.kafka.status")
                    .isEqualTo("UP");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.components.kafka.details.nodes")
                    .isEqualTo(1);
        });
    }

    @Test
    void studentAndAnonymousSeeOnlyTheAggregateStatus() {
        String studentToken = login("deniz");

        await().atMost(INFRASTRUCTURE_READY_TIMEOUT).untilAsserted(() -> {
            assertThat(health(studentToken)).bodyJson().doesNotHavePath("$.components");
            assertThat(health(null)).bodyJson().doesNotHavePath("$.components");
            assertThat(health(null)).bodyJson().extractingPath("$.status").isEqualTo("UP");
        });
    }

    private MvcTestResult health(String token) {
        var request = mvc.get().uri("/actuator/health");
        return token == null
                ? request.exchange()
                : request.header("Authorization", "Bearer " + token).exchange();
    }

    private String login(String slug) {
        MvcTestResult result = mvc.post()
                .uri("/api/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"" + slug + "\"}")
                .exchange();
        return json.readTree(result.getResponse().getContentAsByteArray())
                .get("token")
                .asString();
    }
}
