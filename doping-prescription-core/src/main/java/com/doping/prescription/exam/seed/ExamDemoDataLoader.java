package com.doping.prescription.exam.seed;

import com.doping.prescription.exam.domain.ExamOption;
import com.doping.prescription.shared.api.DemoDataLoader;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seeds the two mini exams from {@code seed/exams.json} (docs/seed-verisi.md). Question id = exam id × 100 + order.
 * Texts refresh on every run; is_active is written on first insert only.
 */
@Component
@Order(3)
public class ExamDemoDataLoader implements DemoDataLoader {

    private static final String SEED_FILE = "seed/exams.json";
    private static final String EXAM_TYPE = "TYT";
    private static final int QUESTION_ID_FACTOR = 100;
    private static final String OPTION_KEYS = "ABCDE";

    private static final String UPSERT_EXAM = """
            insert into exam (id, title, exam_type, duration_min, is_active, updated_at)
            values (:id, :title, :examType, :durationMin, true, :now)
            on conflict (id) do update set
                title = excluded.title, duration_min = excluded.duration_min, updated_at = excluded.updated_at
            """;

    private static final String UPSERT_QUESTION = """
            insert into exam_question (id, exam_id, order_no, topic_id, stem, options, correct_key, explanation,
                                       scenario_answer, difficulty, updated_at)
            values (:id, :examId, :orderNo, :topicId, :stem, cast(:options as jsonb), :correctKey, :explanation,
                    :scenarioAnswer, :difficulty, :now)
            on conflict (id) do update set
                topic_id = excluded.topic_id, stem = excluded.stem, options = excluded.options,
                correct_key = excluded.correct_key, explanation = excluded.explanation,
                scenario_answer = excluded.scenario_answer, difficulty = excluded.difficulty,
                updated_at = excluded.updated_at
            """;

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    ExamDemoDataLoader(JdbcClient jdbc, JsonMapper jsonMapper, Clock clock) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "exam";
    }

    @Override
    @Transactional
    public void load() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (SeedExam exam : readExams()) {
            jdbc.sql(UPSERT_EXAM)
                    .param("id", exam.id())
                    .param("title", exam.title())
                    .param("examType", EXAM_TYPE)
                    .param("durationMin", exam.durationMin())
                    .param("now", now)
                    .update();
            exam.questions().forEach(question -> upsertQuestion(exam.id(), question, now));
        }
        moveIdentityPastSeededIds("exam");
        moveIdentityPastSeededIds("exam_question");
    }

    private void upsertQuestion(int examId, SeedQuestion question, OffsetDateTime now) {
        jdbc.sql(UPSERT_QUESTION)
                .param("id", examId * QUESTION_ID_FACTOR + question.orderNo())
                .param("examId", examId)
                .param("orderNo", question.orderNo())
                .param("topicId", question.topicId())
                .param("stem", question.stem())
                .param("options", jsonMapper.writeValueAsString(lettered(question.options())))
                .param("correctKey", question.correctKey())
                .param("explanation", question.explanation())
                .param("scenarioAnswer", question.scenarioAnswer())
                .param("difficulty", question.difficulty())
                .param("now", now)
                .update();
    }

    private static List<ExamOption> lettered(List<String> optionTexts) {
        return IntStream.range(0, optionTexts.size())
                .mapToObj(index -> new ExamOption(String.valueOf(OPTION_KEYS.charAt(index)), optionTexts.get(index)))
                .toList();
    }

    private void moveIdentityPastSeededIds(String table) {
        jdbc.sql("select setval(pg_get_serial_sequence('" + table + "', 'id'), (select max(id) from " + table + "))")
                .query()
                .singleValue();
    }

    private List<SeedExam> readExams() {
        try (InputStream json = new ClassPathResource(SEED_FILE).getInputStream()) {
            return jsonMapper.readerForListOf(SeedExam.class).at("/items").readValue(json);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + SEED_FILE, e);
        }
    }

    /** One exam of exams.json. */
    record SeedExam(int id, String title, int durationMin, List<SeedQuestion> questions) {}

    /**
     * One question of an exam. The file's {@code check} field is for the seed verification test only.
     *
     * @param options        option texts in A-E order
     * @param scenarioAnswer answer given in the demo scenario; {@code null} = left blank
     */
    @JsonIgnoreProperties("check")
    record SeedQuestion(
            int orderNo,
            int topicId,
            int difficulty,
            String correctKey,
            String scenarioAnswer,
            String stem,
            List<String> options,
            String explanation) {}
}
