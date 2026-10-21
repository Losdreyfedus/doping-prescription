package com.doping.prescription;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/** PostgreSQL plus Redis and Kafka, for tests that talk to the cache or the broker. */
@TestConfiguration(proxyBeanMethods = false)
public class InfrastructureContainersConfiguration {

    @Bean
    DynamicPropertyRegistrar infrastructureProperties() {
        return registry -> {
            TestContainers.registerPostgres(registry);
            registry.add("spring.data.redis.host", () -> TestContainers.redis().getHost());
            registry.add(
                    "spring.data.redis.port", () -> TestContainers.redis().getMappedPort(TestContainers.REDIS_PORT));
            registry.add(
                    "spring.kafka.bootstrap-servers",
                    () -> TestContainers.kafka().getBootstrapServers());
        };
    }
}
