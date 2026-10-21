package com.doping.prescription.shared.api;

/**
 * Loads one module's demo seed data (docs/seed-verisi.md). Every module seeds only its own tables; the admin module
 * runs the loaders in {@link org.springframework.core.annotation.Order} sequence. Implementations must be idempotent:
 * running them again never duplicates rows.
 */
public interface DemoDataLoader {

    /** Short name for logs, e.g. {@code content}. */
    String name();

    void load();
}
