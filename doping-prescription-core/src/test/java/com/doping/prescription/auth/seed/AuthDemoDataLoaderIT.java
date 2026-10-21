package com.doping.prescription.auth.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.domain.Role;
import com.doping.prescription.auth.persistence.UserAccount;
import com.doping.prescription.auth.persistence.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class AuthDemoDataLoaderIT {

    private static final List<String> DEMO_SLUGS = List.of("deniz", "mert", "selin", "admin");
    private static final int EXAM_IN_DAYS = 240;

    @Autowired
    private AuthDemoDataLoader loader;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private Clock clock;

    @Test
    void seedsFourDemoUsersAndDoesNotDuplicateThemOnRerun() {
        // When
        loader.load();
        List<UserAccount> firstRun = demoUsers();
        loader.load();

        // Then
        assertThat(demoUsers()).hasSize(4);
        assertThat(demoUsers())
                .extracting(UserAccount::getId)
                .containsExactlyInAnyOrderElementsOf(
                        firstRun.stream().map(UserAccount::getId).toList());
        UserAccount deniz = users.findBySlug("deniz").orElseThrow();
        assertThat(deniz.getRole()).isEqualTo(Role.STUDENT);
        assertThat(deniz.getExamDate()).contains(LocalDate.now(clock).plusDays(EXAM_IN_DAYS));
        assertThat(users.findBySlug("selin").orElseThrow().isPersonalizationEnabled())
                .isFalse();
        UserAccount admin = users.findBySlug("admin").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getExamDate()).isEmpty();
    }

    @Test
    void rerunKeepsPersonalizationSettingChangedSinceFirstSeed() {
        // Given
        loader.load();
        transaction.executeWithoutResult(status -> {
            UserAccount mert = users.findBySlug("mert").orElseThrow();
            mert.changeSettings(false, mert.getExamDate().orElseThrow(), clock.instant());
        });

        // When
        loader.load();

        // Then
        assertThat(users.findBySlug("mert").orElseThrow().isPersonalizationEnabled())
                .isFalse();

        // restore the seeded value for other tests
        transaction.executeWithoutResult(status -> {
            UserAccount mert = users.findBySlug("mert").orElseThrow();
            mert.changeSettings(true, mert.getExamDate().orElseThrow(), clock.instant());
        });
    }

    private List<UserAccount> demoUsers() {
        return users.findAll().stream()
                .filter(user -> DEMO_SLUGS.contains(user.getSlug()))
                .toList();
    }
}
