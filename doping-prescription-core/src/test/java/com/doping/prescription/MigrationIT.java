package com.doping.prescription;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Applies all Flyway migrations to an empty PostgreSQL. Starting the context also runs Hibernate's schema validation
 * ({@code ddl-auto=validate}), so every entity, including Spring Modulith's event publication, matches the migrations.
 */
@IntegrationTest
class MigrationIT {

    /** Every table from docs/veri-modeli.md plus Spring Modulith's outbox table. */
    static final List<String> EXPECTED_TABLES = List.of("processed_event", "event_publication", "users");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void appliesAllMigrationsSuccessfully() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).isNotEmpty();
        assertThat(Arrays.stream(applied).map(MigrationInfo::getState)).containsOnly(MigrationState.SUCCESS);
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void createsEveryTableOfTheDataModel() {
        List<String> tables = jdbc.sql("select table_name from information_schema.tables where table_schema = 'public'")
                .query(String.class)
                .list();

        assertThat(tables).containsAll(EXPECTED_TABLES);
    }
}
