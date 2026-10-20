package com.doping.prescription.shared.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaAdmin;

class KafkaHealthIndicatorTest {

    // Nothing listens on port 1, so the broker is guaranteed to be unreachable.
    private static final String UNREACHABLE_BROKER = "localhost:1";

    @Test
    void reportsDownWithinTimeoutWhenBrokerIsUnreachable() {
        // Given
        KafkaAdmin kafkaAdmin = new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, UNREACHABLE_BROKER));
        KafkaHealthIndicator indicator =
                new KafkaHealthIndicator(kafkaAdmin, new KafkaHealthProperties(Duration.ofMillis(500)));

        // When
        long startedAt = System.nanoTime();
        Health health = indicator.health();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
    }
}
