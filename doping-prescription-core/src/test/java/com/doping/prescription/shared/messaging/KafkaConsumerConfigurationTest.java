package com.doping.prescription.shared.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Poison messages go to the DLT at once; other failures are retried first (docs/mimari.md §3). */
class KafkaConsumerConfigurationTest {

    private final ConsumerRecordRecoverer recoverer = mock(ConsumerRecordRecoverer.class);
    private final DefaultErrorHandler handler = KafkaConsumerConfiguration.errorHandler(recoverer);
    private final ConsumerRecord<String, byte[]> record =
            new ConsumerRecord<>("exam.result-published", 0, 42L, "user", "{".getBytes());

    @Test
    void unreadableJsonGoesToTheDeadLetterTopicWithoutRetries() {
        // Given: the listener failed to parse the payload
        Throwable parseError = catchThrowable(() -> JsonMapper.builder().build().readTree("{"));
        assertThat(parseError).isInstanceOf(JacksonException.class);

        // When
        boolean recovered = handle((JacksonException) parseError);

        // Then
        assertThat(recovered).isTrue();
        verify(recoverer).accept(same(record), any());
    }

    @Test
    void anInvalidPayloadGoesToTheDeadLetterTopicWithoutRetries() {
        // When
        boolean recovered = handle(new IllegalArgumentException("Unknown exam answer result: MAYBE"));

        // Then
        assertThat(recovered).isTrue();
        verify(recoverer).accept(same(record), any());
    }

    @Test
    void aTransientFailureIsRetriedBeforeTheDeadLetterTopic() {
        // When
        boolean recovered = handle(new DataAccessResourceFailureException("database down"));

        // Then
        assertThat(recovered).isFalse();
        verify(recoverer, never()).accept(any(), any());
    }

    /** As the listener container reports it: the listener's exception wrapped in a ListenerExecutionFailedException. */
    private boolean handle(Exception listenerFailure) {
        return handler.handleOne(
                new ListenerExecutionFailedException("listener failed", listenerFailure),
                record,
                mock(Consumer.class),
                mock(MessageListenerContainer.class));
    }
}
