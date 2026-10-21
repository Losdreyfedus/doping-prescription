package com.doping.prescription.content.seed;

import com.doping.prescription.content.application.ContentCreated;
import com.doping.prescription.content.domain.AnswerOption;
import com.doping.prescription.content.domain.ContentKind;
import com.doping.prescription.content.domain.ContentStatus;
import com.doping.prescription.shared.api.DemoDataLoader;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seeds topics, the keyword dictionary and the 40 videos (docs/seed-verisi.md). Rows are upserted by their fixed ids:
 * texts and numbers are refreshed on every run, status and approval only on first insert, so editor actions survive a
 * restart. Untagged (DRAFT) videos publish {@link ContentCreated} once, when they are first inserted, which runs the
 * normal keyword tagging.
 */
@Component
@Order(2)
public class ContentDemoDataLoader implements DemoDataLoader {

    private static final String SUBJECT = "Matematik";
    private static final String EXAM_TYPE = "TYT";

    /** The seeded admin user (seed/users.json); the content module only stores the id (ADR 0005). */
    private static final UUID SEED_EDITOR_ID = UUID.fromString("00000000-0000-4000-8000-000000000004");

    private static final String UPSERT_TOPIC = """
            insert into topic (id, code, name, exam_type, subject, exam_weight, sort_order, updated_at)
            values (:id, :code, :name, :examType, :subject, :examWeight, :id, :now)
            on conflict (id) do update set
                code = excluded.code, name = excluded.name, exam_weight = excluded.exam_weight,
                sort_order = excluded.sort_order, updated_at = excluded.updated_at
            """;

    private static final String UPSERT_KEYWORD = """
            insert into topic_keyword (topic_id, keyword, weight, is_negative)
            values (:topicId, :keyword, :weight, false)
            on conflict (topic_id, keyword) do update set weight = excluded.weight
            """;

    /** Returns whether the row was inserted (xmax = 0) rather than updated. */
    private static final String UPSERT_VIDEO = """
            insert into content_item (id, title, description, teacher_name, duration_sec, difficulty, content_kind,
                                      quality_score, status, published_at, updated_at)
            values (:id, :title, :description, :teacherName, :durationSec, :difficulty, :kind,
                    :qualityScore, :status, :publishedAt, :now)
            on conflict (id) do update set
                title = excluded.title, description = excluded.description, teacher_name = excluded.teacher_name,
                duration_sec = excluded.duration_sec, difficulty = excluded.difficulty,
                content_kind = excluded.content_kind, quality_score = excluded.quality_score,
                published_at = coalesce(excluded.published_at, content_item.published_at),
                updated_at = excluded.updated_at
            returning (xmax = 0) as inserted
            """;

    private static final String INSERT_MANUAL_LINK = """
            insert into content_topic_map (content_id, topic_id, weight, tag_source, confidence, approved_by,
                                           approved_at, updated_at)
            values (:contentId, :topicId, 1.00, 'MANUAL', 1.00, :editor, :now, :now)
            on conflict (content_id, topic_id) do nothing
            """;

    /** Status is written on first insert only; texts are refreshed. Options are stored as jsonb. */
    private static final String UPSERT_MICRO_QUESTION = """
            insert into micro_question (id, content_id, topic_id, stem, options, correct_key, explanation, difficulty,
                                        status, updated_at)
            values (:id, :contentId, :topicId, :stem, cast(:options as jsonb), :correctKey, :explanation, :difficulty,
                    'LIVE', :now)
            on conflict (id) do update set
                content_id = excluded.content_id, topic_id = excluded.topic_id, stem = excluded.stem,
                options = excluded.options, correct_key = excluded.correct_key, explanation = excluded.explanation,
                difficulty = excluded.difficulty, updated_at = excluded.updated_at
            """;

    private static final String OPTION_KEYS = "ABCDE";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ContentDemoDataLoader(JdbcClient jdbc, JsonMapper jsonMapper, ApplicationEventPublisher events, Clock clock) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "content";
    }

    @Override
    @Transactional
    public void load() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        readItems("seed/topics.json", SeedTopic.class).forEach(topic -> upsertTopic(topic, now));
        readItems("seed/videos.json", SeedVideo.class).forEach(video -> upsertVideo(video, now));
        readItems("seed/micro-questions.json", SeedMicroQuestion.class)
                .forEach(question -> upsertMicroQuestion(question, now));
        moveIdentityPastSeededIds("topic");
        moveIdentityPastSeededIds("content_item");
        moveIdentityPastSeededIds("micro_question");
    }

    private void upsertMicroQuestion(SeedMicroQuestion question, OffsetDateTime now) {
        jdbc.sql(UPSERT_MICRO_QUESTION)
                .param("id", question.id())
                .param("contentId", question.contentId())
                .param("topicId", question.topicId())
                .param("stem", question.stem())
                .param("options", jsonMapper.writeValueAsString(lettered(question.options())))
                .param("correctKey", question.correctKey())
                .param("explanation", question.explanation())
                .param("difficulty", question.difficulty())
                .param("now", now)
                .update();
    }

    /** The seed file lists option texts in order; they are stored with their letters A, B, C, ... */
    private static List<AnswerOption> lettered(List<String> optionTexts) {
        return IntStream.range(0, optionTexts.size())
                .mapToObj(index -> new AnswerOption(String.valueOf(OPTION_KEYS.charAt(index)), optionTexts.get(index)))
                .toList();
    }

    private void upsertTopic(SeedTopic topic, OffsetDateTime now) {
        jdbc.sql(UPSERT_TOPIC)
                .param("id", topic.id())
                .param("code", topic.code())
                .param("name", topic.name())
                .param("examType", EXAM_TYPE)
                .param("subject", SUBJECT)
                .param("examWeight", topic.examWeight())
                .param("now", now)
                .update();
        for (SeedKeyword keyword : topic.keywords()) {
            jdbc.sql(UPSERT_KEYWORD)
                    .param("topicId", topic.id())
                    .param("keyword", keyword.keyword())
                    .param("weight", keyword.weight())
                    .update();
        }
    }

    private void upsertVideo(SeedVideo video, OffsetDateTime now) {
        boolean inserted = jdbc.sql(UPSERT_VIDEO)
                .param("id", video.id())
                .param("title", video.title())
                .param("description", video.description())
                .param("teacherName", video.teacherName())
                .param("durationSec", video.durationSec())
                .param("difficulty", video.difficulty())
                .param("kind", video.kind().name())
                .param("qualityScore", video.qualityScore())
                .param("status", video.status().name())
                .param("publishedAt", video.publishedDaysAgo() == null ? null : now.minusDays(video.publishedDaysAgo()))
                .param("now", now)
                .query(Boolean.class)
                .single();
        if (video.status() == ContentStatus.LIVE) {
            jdbc.sql(INSERT_MANUAL_LINK)
                    .param("contentId", video.id())
                    .param("topicId", video.topicId())
                    .param("editor", SEED_EDITOR_ID)
                    .param("now", now)
                    .update();
        } else if (inserted) {
            events.publishEvent(new ContentCreated(video.id()));
        }
    }

    /** Ids are written explicitly; later inserts (e.g. admin-created videos) must continue after them. */
    private void moveIdentityPastSeededIds(String table) {
        jdbc.sql("select setval(pg_get_serial_sequence('" + table + "', 'id'), (select max(id) from " + table + "))")
                .query()
                .singleValue();
    }

    private <T> List<T> readItems(String path, Class<T> type) {
        try (InputStream json = new ClassPathResource(path).getInputStream()) {
            return jsonMapper.readerForListOf(type).at("/items").readValue(json);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }

    /**
     * One entry of micro-questions.json. The file's {@code check} field is for the seed verification test only.
     *
     * @param options option texts in A-D order
     */
    @JsonIgnoreProperties("check")
    record SeedMicroQuestion(
            Integer id,
            Integer contentId,
            Integer topicId,
            int difficulty,
            String stem,
            List<String> options,
            String correctKey,
            String explanation) {}

    /** One entry of topics.json. */
    record SeedTopic(Integer id, String code, String name, BigDecimal examWeight, List<SeedKeyword> keywords) {}

    /** A keyword of a seeded topic, already normalized (docs/algoritma.md §5). */
    record SeedKeyword(String keyword, BigDecimal weight) {}

    /**
     * One entry of videos.json.
     *
     * @param status           LIVE (approved, manually tagged) or DRAFT (untagged, auto-tagged after insert)
     * @param publishedDaysAgo publication date relative to the seed day; {@code null} for unpublished drafts
     */
    record SeedVideo(
            Integer id,
            Integer topicId,
            ContentStatus status,
            ContentKind kind,
            int difficulty,
            int durationSec,
            BigDecimal qualityScore,
            Integer publishedDaysAgo,
            String teacherName,
            String title,
            String description) {}
}
