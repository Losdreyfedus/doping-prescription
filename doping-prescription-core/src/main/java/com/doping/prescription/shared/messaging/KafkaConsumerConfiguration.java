package com.doping.prescription.shared.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Error handling of every Kafka listener (docs/mimari.md §3): three retries with exponential backoff, then the record
 * goes to {@code <topic>-dlt} and the failure is logged by the error handler.
 */
@Configuration(proxyBeanMethods = false)
class KafkaConsumerConfiguration {

    static final int MAX_RETRIES = 3;
    private static final long INITIAL_INTERVAL_MS = 200;
    private static final double MULTIPLIER = 2.0;

    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaTemplate) {
        ExponentialBackOff backOff = new ExponentialBackOff(INITIAL_INTERVAL_MS, MULTIPLIER);
        backOff.setMaxAttempts(MAX_RETRIES);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);
    }
}
