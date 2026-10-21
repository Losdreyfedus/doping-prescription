package com.doping.prescription.auth.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read access to users for other modules (admin demo control, personas). */
public interface UserDirectory {

    /** All users, demo students first and admin last. */
    List<UserSummary> allUsers();

    Optional<UserSummary> findBySlug(String slug);

    /**
     * @param userId                 the user's id
     * @param slug                   demo login name
     * @param displayName            name shown in the UI
     * @param role                   {@code STUDENT} or {@code ADMIN}
     * @param personalizationEnabled whether the feed is personalized for this user
     */
    record UserSummary(UUID userId, String slug, String displayName, String role, boolean personalizationEnabled) {}
}
