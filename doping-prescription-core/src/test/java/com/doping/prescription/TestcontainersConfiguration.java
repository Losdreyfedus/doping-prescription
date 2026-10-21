package com.doping.prescription;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL, Redis and Kafka for integration tests. Image versions must match docker-compose.yml so tests run against
 * the same infrastructure as the demo.
 *
 * <p>The containers are JVM-wide singletons started once, shared by every Spring test context (contexts differ when
 * test classes use different properties or module slices). They are deliberately not Spring beans: Spring Boot stops
 * container beans when a context closes, which would leave the other cached contexts' clients hanging on shutdown.
 * Testcontainers' Ryuk removes the containers when the JVM exits.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final int REDIS_PORT = 6379;

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"));
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4.11-alpine")).withExposedPorts(REDIS_PORT);
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.2"));

    static {
        Startables.deepStart(POSTGRES, REDIS, KAFKA).join();
    }

    @Bean
    DynamicPropertyRegistrar containerProperties() {
        return registry -> {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.data.redis.host", REDIS::getHost);
            registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
            registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        };
    }
}
