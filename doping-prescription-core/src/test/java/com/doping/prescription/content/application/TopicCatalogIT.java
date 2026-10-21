package com.doping.prescription.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.content.api.TopicCatalog;
import com.doping.prescription.content.seed.ContentDemoDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class TopicCatalogIT {

    @Autowired
    private TopicCatalog catalog;

    @Autowired
    private ContentDemoDataLoader content;

    @Test
    void servesSeededTopicsInCurriculumOrder() {
        content.load();

        assertThat(catalog.allTopics())
                .filteredOn(topic -> topic.code().startsWith("TYT.MAT."))
                .extracting(TopicCatalog.TopicView::topicId)
                .startsWith(1, 2, 3);
        assertThat(catalog.topicsById().get(9)).satisfies(topic -> {
            assertThat(topic.name()).isEqualTo("Olasılık");
            assertThat(topic.examWeight()).isEqualTo(0.5);
        });
    }
}
