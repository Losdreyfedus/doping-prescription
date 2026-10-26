package com.doping.prescription.shared.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A short pause after a failure of an optional dependency: while it lasts, callers skip the dependency instead of
 * waiting for its timeout on every call (e.g. Redis, docs/mimari.md §5). Thread-safe; time comes from the given clock.
 */
public final class Cooldown {

    private final Clock clock;
    private final Duration length;
    private final AtomicReference<Instant> until = new AtomicReference<>(Instant.MIN);

    public Cooldown(Clock clock, Duration length) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.length = Objects.requireNonNull(length, "length");
        if (length.isNegative()) {
            throw new IllegalArgumentException("length must not be negative");
        }
    }

    /** True while the pause started by the latest failure lasts. */
    public boolean active() {
        return clock.instant().isBefore(until.get());
    }

    /** Starts the pause, or restarts it from now. */
    public void start() {
        until.set(clock.instant().plus(length));
    }
}
