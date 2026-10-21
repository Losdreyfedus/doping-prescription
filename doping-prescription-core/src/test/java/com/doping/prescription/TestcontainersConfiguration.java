package com.doping.prescription;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * PostgreSQL for integration tests. Redis and Kafka are not started: the application connects to them lazily, so tests
 * that only touch the database never pay for (or depend on) those containers. Tests that need them use
 * {@link InfrastructureContainersConfiguration} instead.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final int UNREACHABLE_PORT = 1;

    @Bean
    DynamicPropertyRegistrar postgresOnlyProperties() {
        return registry -> {
            TestContainers.registerPostgres(registry);
            // Deliberately unreachable: a database-only test that touches Redis or Kafka fails loudly instead of
            // silently using a locally running docker compose stack.
            registry.add("spring.data.redis.port", () -> UNREACHABLE_PORT);
            registry.add("spring.kafka.bootstrap-servers", () -> "localhost:" + UNREACHABLE_PORT);
            // Without a broker: skip topic creation at startup and give up sending quickly instead of after 60 s.
            registry.add("spring.kafka.admin.auto-create", () -> "false");
            registry.add("spring.kafka.producer.properties.max.block.ms", () -> "1000");
        };
    }
}
