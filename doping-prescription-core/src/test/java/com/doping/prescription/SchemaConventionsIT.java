package com.doping.prescription;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.auth.domain.Role;
import com.doping.prescription.auth.domain.TargetExam;
import com.doping.prescription.content.domain.ContentKind;
import com.doping.prescription.content.domain.ContentStatus;
import com.doping.prescription.content.domain.MicroQuestionStatus;
import com.doping.prescription.content.domain.TagSource;
import com.doping.prescription.exam.domain.AnswerResult;
import com.doping.prescription.exam.domain.AttemptStatus;
import com.doping.prescription.feed.domain.CandidatePool;
import com.doping.prescription.feed.domain.InteractionType;
import com.doping.prescription.learner.domain.SignalType;
import com.doping.prescription.points.domain.PointsReason;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Checks the conventions of docs/veri-modeli.md against the migrated schema, so a future migration cannot silently
 * drop them.
 */
@IntegrationTest
class SchemaConventionsIT {

    /** Owned by Spring Modulith; its schema is copied verbatim and not ours to change. */
    private static final String MODULITH_TABLE = "event_publication";

    /** Id columns that are deliberately not the leading column of an index, with the reason. */
    private static final Set<String> UNINDEXED_ID_COLUMNS = Set.of(
            // second column of processed_event's primary key; lookups always include the consumer
            "processed_event.event_id",
            // only read together with its learner_topic_mastery row, never queried on its own
            "learner_topic_mastery.last_exam_attempt_id",
            // only compared with the incoming ref id of the same streak row
            "points_streak.last_ref_id");

    private static final Pattern QUOTED_VALUE = Pattern.compile("'([A-Z_]+)'");

    @Autowired
    private JdbcClient jdbc;

    @Test
    void everyTableHasNotNullCreatedAt() {
        List<String> tablesWithoutCreatedAt = jdbc.sql("""
                        select t.table_name from information_schema.tables t
                        where t.table_schema = 'public' and t.table_type = 'BASE TABLE'
                          and t.table_name not in ('flyway_schema_history', :modulithTable)
                          and not exists (
                            select 1 from information_schema.columns c
                            where c.table_schema = 'public' and c.table_name = t.table_name
                              and c.column_name = 'created_at' and c.is_nullable = 'NO'
                              and c.data_type = 'timestamp with time zone')
                        """)
                .param("modulithTable", MODULITH_TABLE)
                .query(String.class)
                .list();

        assertThat(tablesWithoutCreatedAt).isEmpty();
        // guard against a query that silently matches nothing
        assertThat(jdbc.sql("""
                        select count(*) from information_schema.columns
                        where table_schema = 'public' and column_name = 'created_at'
                          and table_name <> 'flyway_schema_history'
                        """).query(Integer.class).single()).isEqualTo(MigrationIT.EXPECTED_TABLES.size() - 1);
    }

    @Test
    void everyIdColumnLeadsAnIndex() {
        List<String> idColumns = jdbc.sql("""
                        select c.table_name || '.' || c.column_name from information_schema.columns c
                        where c.table_schema = 'public' and c.column_name like '%\\_id'
                          and c.table_name not in ('flyway_schema_history', :modulithTable)
                        """)
                .param("modulithTable", MODULITH_TABLE)
                .query(String.class)
                .list();
        Set<String> leadingIndexColumns =
                Set.copyOf(jdbc.sql("""
                        select t.relname || '.' || a.attname from pg_index i
                        join pg_class t on t.oid = i.indrelid
                        join pg_namespace n on n.oid = t.relnamespace and n.nspname = 'public'
                        join pg_attribute a on a.attrelid = t.oid and a.attnum = i.indkey[0]
                        """).query(String.class).list());

        // guard against a query that silently matches nothing
        assertThat(idColumns)
                .contains("exam_attempt.user_id", "micro_question.content_id", "feed_impression.session_id");
        assertThat(idColumns)
                .filteredOn(column -> !UNINDEXED_ID_COLUMNS.contains(column))
                .allSatisfy(column -> assertThat(leadingIndexColumns).contains(column));
    }

    @ParameterizedTest(name = "{0}.{1} matches {2}")
    @MethodSource("enumConstraints")
    void enumCheckConstraintMatchesJavaEnum(String table, String constraint, Class<? extends Enum<?>> enumType) {
        String definition = jdbc.sql("""
                        select pg_get_constraintdef(c.oid) from pg_constraint c
                        join pg_class t on t.oid = c.conrelid
                        where t.relname = :table and c.conname = :constraint
                        """)
                .param("table", table)
                .param("constraint", constraint)
                .query(String.class)
                .single();

        assertThat(quotedValues(definition)).isEqualTo(enumNames(enumType));
    }

    static Stream<Arguments> enumConstraints() {
        return Stream.of(
                Arguments.of("users", "ck_users_role", Role.class),
                Arguments.of("users", "ck_users_target_exam", TargetExam.class),
                Arguments.of("topic", "ck_topic_exam_type", com.doping.prescription.content.domain.ExamType.class),
                Arguments.of("content_item", "ck_content_item_kind", ContentKind.class),
                Arguments.of("content_item", "ck_content_item_status", ContentStatus.class),
                Arguments.of("content_topic_map", "ck_content_topic_map_tag_source", TagSource.class),
                Arguments.of("micro_question", "ck_micro_question_status", MicroQuestionStatus.class),
                Arguments.of("exam", "ck_exam_exam_type", com.doping.prescription.exam.domain.ExamType.class),
                Arguments.of("exam_attempt", "ck_exam_attempt_status", AttemptStatus.class),
                Arguments.of("exam_answer", "ck_exam_answer_result", AnswerResult.class),
                Arguments.of("learner_signal", "ck_learner_signal_type", SignalType.class),
                Arguments.of("feed_impression", "ck_feed_impression_pool", CandidatePool.class),
                Arguments.of("feed_interaction", "ck_feed_interaction_event", InteractionType.class),
                Arguments.of("points_ledger", "ck_points_ledger_reason", PointsReason.class));
    }

    private static Set<String> quotedValues(String constraintDefinition) {
        Matcher matcher = QUOTED_VALUE.matcher(constraintDefinition);
        Set<String> values = new HashSet<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static Set<String> enumNames(Class<? extends Enum<?>> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(Enum::name).collect(Collectors.toSet());
    }
}
