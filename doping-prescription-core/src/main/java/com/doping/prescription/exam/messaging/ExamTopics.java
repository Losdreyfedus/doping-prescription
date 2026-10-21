package com.doping.prescription.exam.messaging;

import com.doping.prescription.exam.api.ExamResultPublished;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topics owned by the exam module, created at startup (docs/api.md, Event'ler: 3 partitions, replication 1). */
@Configuration(proxyBeanMethods = false)
class ExamTopics {

    private static final int PARTITIONS = 3;

    @Bean
    NewTopic examResultPublishedTopic() {
        return TopicBuilder.name(ExamResultPublished.TOPIC)
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }
}
