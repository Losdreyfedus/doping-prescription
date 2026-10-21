package com.doping.prescription.content.api;

import java.util.List;
import java.util.Map;

/** Read access to the curriculum topics for other modules (docs/mimari.md §1). */
public interface TopicCatalog {

    /** All topics in curriculum order. */
    List<TopicView> allTopics();

    /** Topics by id, for labelling results and profiles. */
    Map<Integer, TopicView> topicsById();

    /**
     * @param topicId    the topic's id
     * @param code       stable code, e.g. {@code TYT.MAT.OLAS}
     * @param name       Turkish display name
     * @param examWeight the topic's weight in the exam, 0-1
     */
    record TopicView(Integer topicId, String code, String name, double examWeight) {}
}
