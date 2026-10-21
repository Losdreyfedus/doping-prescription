package com.doping.prescription.auth.api;

import java.util.Objects;
import java.util.UUID;

/**
 * The authenticated user of the current request. Controllers of any module declare it as a method parameter; the user
 * id always comes from the token, never from the request body (docs/api.md).
 *
 * @param userId the user's id
 * @param slug   demo login name, e.g. {@code deniz}
 * @param admin  whether the user has the ADMIN role
 */
public record CurrentUser(UUID userId, String slug, boolean admin) {

    public CurrentUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(slug, "slug");
    }
}
