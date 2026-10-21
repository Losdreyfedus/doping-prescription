package com.doping.prescription.admin.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.shared.api.DemoDataLoader;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

/**
 * Phase 1 acceptance (AGENTS.md §7): starting with the demo profile seeds 10 topics, 40 videos, 2 mini exams and 4
 * users, and running the seed again does not duplicate anything.
 */
@IntegrationTest
@ActiveProfiles("demo")
class DemoSeedIT {

    private static final Duration TAGGING_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private List<DemoDataLoader> loaders;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void loadersRunInModuleOrder() {
        assertThat(loaders).extracting(DemoDataLoader::name).containsExactly("auth", "content", "exam");
    }

    @Test
    void startupSeedMatchesAcceptanceCountsAndRerunDoesNotDuplicate() {
        // Given: the seeder already ran at startup; the untagged videos are tagged asynchronously
        awaitTagging();
        SeedCounts afterStartup = counts();

        // When
        seeder.run(new DefaultApplicationArguments());
        awaitTagging();

        // Then
        assertThat(afterStartup).isEqualTo(new SeedCounts(10, 40, 36, 4, 48, 2, 24, 4));
        assertThat(counts()).isEqualTo(afterStartup);
    }

    private void awaitTagging() {
        await().atMost(TAGGING_TIMEOUT)
                .until(() ->
                        count("select count(*) from content_item where id in (8, 20, 28, 40) and status = 'TAGGED'")
                                == 4);
    }

    private SeedCounts counts() {
        return new SeedCounts(
                count("select count(*) from topic where id between 1 and 10"),
                count("select count(*) from content_item where id between 1 and 40"),
                count("select count(*) from content_item where id between 1 and 40 and status = 'LIVE'"),
                count("select count(*) from content_item where id between 1 and 40 and status = 'TAGGED'"),
                count("select count(*) from micro_question where id between 1 and 48"),
                count("select count(*) from exam where id in (1, 2)"),
                count("select count(*) from exam_question where exam_id in (1, 2)"),
                count("select count(*) from users where slug in ('deniz', 'mert', 'selin', 'admin')"));
    }

    private int count(String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }

    record SeedCounts(
            int topics,
            int videos,
            int liveVideos,
            int taggedVideos,
            int microQuestions,
            int exams,
            int examQuestions,
            int users) {}
}
