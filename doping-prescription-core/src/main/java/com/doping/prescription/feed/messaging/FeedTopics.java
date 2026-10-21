package com.doping.prescription.feed.messaging;

import com.doping.prescription.feed.api.InteractionRecorded;
import com.doping.prescription.learner.api.MasteryChanged;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** The feed module's event topic and the dead-letter topic of its mastery listener (same partition count). */
@Configuration(proxyBeanMethods = false)
class FeedTopics {

    private static final int PARTITIONS = 3;

    @Bean
    NewTopic interactionRecordedTopic() {
        return TopicBuilder.name(InteractionRecorded.TOPIC)
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic masteryChangedDeadLetterTopic() {
        return TopicBuilder.name(MasteryChanged.TOPIC + "-dlt")
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }
}
