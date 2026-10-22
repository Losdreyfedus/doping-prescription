package com.doping.prescription.content.application;

import com.doping.prescription.content.api.ContentCatalog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves {@link ContentCatalog} with aggregate queries (no N+1). */
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

    private static final String VIDEOS = """
            select item.id, item.title, item.teacher_name, item.video_url, item.duration_sec, item.content_kind,
                   item.difficulty, item.quality_score, primary_topic.topic_id,
                   exists (select 1 from micro_question question
                           where question.content_id = item.id and question.status = 'LIVE') as has_micro_quiz
            from content_item item
            join (
                select distinct on (content_id) content_id, topic_id
                from content_topic_map
                order by content_id, weight desc, topic_id
            ) primary_topic on primary_topic.content_id = item.id
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

    @Override
    @Transactional(readOnly = true)
    public List<VideoView> liveVideos() {
        return jdbc.sql(VIDEOS + " where item.status = 'LIVE' order by item.id")
                .query((row, rowNumber) -> videoOf(row))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoView> videos(Collection<Integer> contentIds) {
        if (contentIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(VIDEOS + " where item.id in (:contentIds) order by item.id")
                .param("contentIds", contentIds)
                .query((row, rowNumber) -> videoOf(row))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoView> video(Integer contentId) {
        return jdbc.sql(VIDEOS + " where item.id = :contentId")
                .param("contentId", contentId)
                .query((row, rowNumber) -> videoOf(row))
                .optional();
    }

    private static VideoView videoOf(ResultSet row) throws SQLException {
        return new VideoView(
                row.getInt("id"),
                row.getString("title"),
                row.getString("teacher_name"),
                row.getString("video_url"),
                row.getInt("duration_sec"),
                row.getString("content_kind"),
                row.getInt("difficulty"),
                row.getDouble("quality_score"),
                row.getInt("topic_id"),
                row.getBoolean("has_micro_quiz"));
    }
}
