package com.doping.prescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthEndpointIT {

    // A freshly started Kafka container may need a few seconds before it answers metadata requests.
    private static final Duration INFRASTRUCTURE_READY_TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private MockMvcTester mvc;

    @Test
    void reportsPostgresRedisAndKafkaAsUp() {
        await().atMost(INFRASTRUCTURE_READY_TIMEOUT).untilAsserted(() -> {
            // When
            MvcTestResult result = mvc.get().uri("/actuator/health").exchange();

            // Then
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
}
