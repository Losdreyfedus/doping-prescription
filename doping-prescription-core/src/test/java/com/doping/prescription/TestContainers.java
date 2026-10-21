package com.doping.prescription;

import java.time.Duration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * JVM-wide singleton containers for integration tests. Image versions must match docker-compose.yml.
 *
 * <p>Each container lives in its own holder class, so it is started on first use only: a test run that needs just
 * PostgreSQL never starts Kafka. The containers are not Spring beans; Spring Boot stops container beans when a context
 * closes, which would break the other cached contexts. Testcontainers' Ryuk removes them when the JVM exits.
 */
final class TestContainers {

    static final int REDIS_PORT = 6379;

    /**
     * A single-node KRaft broker can take well over the 60 s Testcontainers default to log readiness on a loaded
     * Docker host (observed right after a Docker Desktop restart with three containers starting at once).
     */
    private static final Duration KAFKA_STARTUP_TIMEOUT = Duration.ofMinutes(3);

    private TestContainers() {}

    static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PostgresHolder.CONTAINER.getJdbcUrl());
        registry.add("spring.datasource.username", () -> PostgresHolder.CONTAINER.getUsername());
        registry.add("spring.datasource.password", () -> PostgresHolder.CONTAINER.getPassword());
    }

    static GenericContainer<?> redis() {
        return RedisHolder.CONTAINER;
    }

    static KafkaContainer kafka() {
        return KafkaHolder.CONTAINER;
    }

    private static final class PostgresHolder {
        static final PostgreSQLContainer CONTAINER =
                start(new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine")));
    }

    private static final class RedisHolder {
        static final GenericContainer<?> CONTAINER = start(
                new GenericContainer<>(DockerImageName.parse("redis:7.4.11-alpine")).withExposedPorts(REDIS_PORT));
    }

    private static final class KafkaHolder {
        static final KafkaContainer CONTAINER = start(new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.2"))
                .withStartupTimeout(KAFKA_STARTUP_TIMEOUT));
    }

    private static <T extends GenericContainer<?>> T start(T container) {
        container.start();
        return container;
    }
}
