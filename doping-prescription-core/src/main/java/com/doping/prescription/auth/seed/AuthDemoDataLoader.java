package com.doping.prescription.auth.seed;

import com.doping.prescription.auth.domain.Role;
import com.doping.prescription.auth.domain.TargetExam;
import com.doping.prescription.shared.api.DemoDataLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Seeds the four demo users from {@code seed/users.json} (docs/seed-verisi.md); upserts by slug. */
@Component
@Order(1)
public class AuthDemoDataLoader implements DemoDataLoader {

    private static final String SEED_FILE = "seed/users.json";

    /**
     * Name, role and exam date are refreshed on every run (the exam date is relative to the seed day);
     * personalization_enabled is a user setting and is written on first insert only.
     */
    private static final String UPSERT = """
            insert into users (id, slug, display_name, role, target_exam, exam_date, personalization_enabled, updated_at)
            values (:id, :slug, :displayName, :role, :targetExam, :examDate, :personalizationEnabled, :now)
            on conflict (slug) do update set
                display_name = excluded.display_name,
                role = excluded.role,
                exam_date = excluded.exam_date,
                updated_at = excluded.updated_at
            """;

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    AuthDemoDataLoader(JdbcClient jdbc, JsonMapper jsonMapper, Clock clock) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "auth";
    }

    @Override
    @Transactional
    public void load() {
        LocalDate today = LocalDate.now(clock);
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (SeedUser user : readUsers()) {
            jdbc.sql(UPSERT)
                    .param("id", user.id())
                    .param("slug", user.slug())
                    .param("displayName", user.displayName())
                    .param("role", user.role().name())
                    .param("targetExam", TargetExam.TYT.name())
                    .param("examDate", user.examInDays() == null ? null : today.plusDays(user.examInDays()))
                    .param("personalizationEnabled", user.personalizationEnabled())
                    .param("now", now)
                    .update();
        }
    }

    private List<SeedUser> readUsers() {
        try (InputStream json = new ClassPathResource(SEED_FILE).getInputStream()) {
            return jsonMapper.readerForListOf(SeedUser.class).at("/items").readValue(json);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + SEED_FILE, e);
        }
    }

    /**
     * One entry of users.json.
     *
     * @param examInDays exam date relative to the seed day, or {@code null} for no exam date
     */
    record SeedUser(
            UUID id, String slug, String displayName, Role role, boolean personalizationEnabled, Integer examInDays) {}
}
