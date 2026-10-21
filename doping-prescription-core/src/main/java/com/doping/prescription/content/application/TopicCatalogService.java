package com.doping.prescription.content.application;

import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.content.persistence.Topic;
import com.doping.prescription.content.persistence.TopicRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves {@link TopicCatalog}; ten small rows, read straight from the database. */
@Service
class TopicCatalogService implements TopicCatalog {

    private final TopicRepository topics;

    TopicCatalogService(TopicRepository topics) {
        this.topics = topics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicView> allTopics() {
        return topics.findAll().stream()
                .sorted(Comparator.comparingInt(Topic::getSortOrder).thenComparing(Topic::getId))
                .map(TopicCatalogService::view)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, TopicView> topicsById() {
        return allTopics().stream().collect(Collectors.toMap(TopicView::topicId, Function.identity()));
    }

    private static TopicView view(Topic topic) {
        return new TopicView(
                topic.getId(),
                topic.getCode(),
                topic.getName(),
                topic.getExamWeight().doubleValue());
    }
}
