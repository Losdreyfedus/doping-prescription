package com.doping.prescription;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private MockMvcTester mvc;

    @Test
    void reportsPostgresRedisAndKafkaAsUp() {
        // When
        MvcTestResult result = mvc.get().uri("/actuator/health").exchange();

        // Then
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("UP");
        assertThat(result).bodyJson().extractingPath("$.components.db.status").isEqualTo("UP");
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
    }
}
