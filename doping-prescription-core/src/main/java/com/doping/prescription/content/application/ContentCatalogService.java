package com.doping.prescription.content.application;

import com.doping.prescription.content.api.ContentCatalog;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves {@link ContentCatalog} with one aggregate query. */
@Service
class ContentCatalogService implements ContentCatalog {

    /** Primary topic per LIVE video (highest weight, then lowest topic id), counted per topic. */
    private static final String LIVE_COUNT_BY_PRIMARY_TOPIC = """
            select topic_id, count(*) as videos
            from (
                select distinct on (map.content_id) map.content_id, map.topic_id
                from content_topic_map map
                join content_item item on item.id = map.content_id
                where item.status = 'LIVE'
                order by map.content_id, map.weight desc, map.topic_id
            ) primary_topic
            group by topic_id
            """;

    private final JdbcClient jdbc;

    ContentCatalogService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Integer> liveVideoCountByTopic() {
        Map<Integer, Integer> counts = new HashMap<>();
        jdbc.sql(LIVE_COUNT_BY_PRIMARY_TOPIC)
                .query((row, rowNumber) -> Map.entry(row.getInt("topic_id"), row.getInt("videos")))
                .list()
                .forEach(entry -> counts.put(entry.getKey(), entry.getValue()));
        return counts;
    }
}
