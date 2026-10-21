package com.doping.prescription.quiz.messaging;

import com.doping.prescription.quiz.api.QuizAnswered;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** The quiz module's analytics topic (docs/api.md, Event'ler). */
@Configuration(proxyBeanMethods = false)
class QuizTopics {

    private static final int PARTITIONS = 3;

    @Bean
    NewTopic quizAnsweredTopic() {
        return TopicBuilder.name(QuizAnswered.TOPIC)
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }
}
