package com.doping.prescription.auth.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.domain.Role;
import com.doping.prescription.auth.domain.TargetExam;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class UserAccountRepositoryIT {

    private static final Instant NOW = Instant.parse("2026-10-20T07:00:00Z");

    @Autowired
    private UserAccountRepository users;

    @Test
    void storesAndFindsUserBySlug() {
        // Given
        UserAccount user = new UserAccount(UUID.randomUUID(), "test-deniz", "Deniz", Role.STUDENT, TargetExam.TYT, NOW);
        user.changeSettings(false, LocalDate.of(2027, 6, 17), NOW);

        // When
        users.saveAndFlush(user);

        // Then
        UserAccount loaded = users.findBySlug("test-deniz").orElseThrow();
        assertThat(loaded.getRole()).isEqualTo(Role.STUDENT);
        assertThat(loaded.isPersonalizationEnabled()).isFalse();
        assertThat(loaded.getExamDate()).contains(LocalDate.of(2027, 6, 17));
    }

    @Test
    void rejectsDuplicateSlug() {
        // Given
        users.saveAndFlush(new UserAccount(UUID.randomUUID(), "test-dup", "A", Role.STUDENT, TargetExam.TYT, NOW));

        // When / Then
        assertThatThrownBy(() -> users.saveAndFlush(
                        new UserAccount(UUID.randomUUID(), "test-dup", "B", Role.ADMIN, TargetExam.TYT, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
