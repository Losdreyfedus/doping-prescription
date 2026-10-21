package com.doping.prescription.learner.messaging;

import com.doping.prescription.exam.api.ExamResultPublished;
import com.doping.prescription.learner.api.MasteryChanged;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topics owned by the learner module: its own event, and the dead-letter topic of its exam-result listener. The DLT
 * has as many partitions as its source, because a dead letter keeps its original partition.
 */
@Configuration(proxyBeanMethods = false)
class LearnerTopics {

    private static final int PARTITIONS = 3;

    @Bean
    NewTopic masteryChangedTopic() {
        return TopicBuilder.name(MasteryChanged.TOPIC)
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic interactionRecordedDeadLetterTopic() {
        return TopicBuilder.name(InteractionListener.TOPIC + "-dlt")
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic examResultDeadLetterTopic() {
        return TopicBuilder.name(ExamResultPublished.TOPIC + "-dlt")
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }
}
