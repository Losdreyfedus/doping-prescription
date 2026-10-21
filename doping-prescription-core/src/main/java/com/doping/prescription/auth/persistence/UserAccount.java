package com.doping.prescription.auth.persistence;

import com.doping.prescription.auth.domain.Role;
import com.doping.prescription.auth.domain.TargetExam;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** A demo user; demo login selects one by {@code slug}. */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;

    private String slug;

    private String displayName;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private TargetExam targetExam;

    private LocalDate examDate;

    private boolean personalizationEnabled;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected UserAccount() {
        // for JPA
    }

    public UserAccount(UUID id, String slug, String displayName, Role role, TargetExam targetExam, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.role = Objects.requireNonNull(role, "role");
        this.targetExam = Objects.requireNonNull(targetExam, "targetExam");
        this.personalizationEnabled = true;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public void changeSettings(boolean personalizationEnabled, LocalDate examDate, Instant now) {
        this.personalizationEnabled = personalizationEnabled;
        this.examDate = examDate;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public TargetExam getTargetExam() {
        return targetExam;
    }

    public Optional<LocalDate> getExamDate() {
        return Optional.ofNullable(examDate);
    }

    public boolean isPersonalizationEnabled() {
        return personalizationEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
