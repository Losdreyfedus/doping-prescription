package com.doping.prescription;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A UTC clock that a test moves by hand, for behaviour that depends on time passing. Not thread-safe. */
public final class MovableClock extends Clock {

    private Instant now;

    public MovableClock(Instant now) {
        this.now = now;
    }

    public void moveTo(Instant instant) {
        now = instant;
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
