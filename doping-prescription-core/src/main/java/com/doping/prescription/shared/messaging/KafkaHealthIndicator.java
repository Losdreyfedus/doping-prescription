package com.doping.prescription.shared.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

/**
 * Reports Kafka in {@code /actuator/health}. Spring Boot ships no Kafka health indicator, so this asks the broker for
 * its cluster description with a short, configurable timeout.
 */
@Component("kafka")
class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private final KafkaHealthProperties properties;

    KafkaHealthIndicator(KafkaAdmin kafkaAdmin, KafkaHealthProperties properties) {
        this.kafkaAdmin = kafkaAdmin;
        this.properties = properties;
    }

    @Override
    public Health health() {
        long timeoutMs = properties.timeout().toMillis();
        try (AdminClient client = AdminClient.create(adminConfig(timeoutMs))) {
            DescribeClusterResult cluster =
                    client.describeCluster(new DescribeClusterOptions().timeoutMs(Math.toIntExact(timeoutMs)));
            String clusterId = cluster.clusterId().get(timeoutMs, TimeUnit.MILLISECONDS);
            int nodeCount =
                    cluster.nodes().get(timeoutMs, TimeUnit.MILLISECONDS).size();
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodes", nodeCount)
                    .build();
        } catch (ExecutionException | TimeoutException e) {
            return Health.down(e).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Health.down(e).build();
        }
    }

    private Map<String, Object> adminConfig(long timeoutMs) {
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        // Bound every admin request so an unreachable broker cannot block the health endpoint.
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, Math.toIntExact(timeoutMs));
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, Math.toIntExact(timeoutMs));
        return config;
    }
}
