package com.doping.prescription.shared.api;

import java.util.UUID;

/**
 * Deletes one learner's activity from one module (docs/mimari.md §1): every module erases only its own tables and
 * Redis keys, and the admin module's demo reset calls all of them. Must be idempotent.
 */
public interface UserDataEraser {

    /** Short name for logs, e.g. {@code feed}. */
    String name();

    void erase(UUID userId);
}
