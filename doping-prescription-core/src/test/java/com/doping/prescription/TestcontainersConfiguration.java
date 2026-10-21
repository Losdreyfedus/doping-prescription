package com.doping.prescription;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL, Redis and Kafka for integration tests. Image versions must match docker-compose.yml so tests run against
 * the same infrastructure as the demo.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final DockerImageName POSTGRES = DockerImageName.parse("postgres:16.15-alpine");
    private static final DockerImageName REDIS = DockerImageName.parse("redis:7.4.11-alpine");
    private static final DockerImageName KAFKA = DockerImageName.parse("apache/kafka:4.3.2");
    private static final int REDIS_PORT = 6379;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES);
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(REDIS).withExposedPorts(REDIS_PORT);
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(KAFKA);
    }
}
