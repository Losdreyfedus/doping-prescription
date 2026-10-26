package com.doping.prescription.shared.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.MovableClock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CooldownTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final Duration LENGTH = Duration.ofSeconds(10);

    @Test
    void isInactiveUntilStarted() {
        assertThat(new Cooldown(clockAt(NOW), LENGTH).active()).isFalse();
    }

    @Test
    void lastsExactlyItsLengthFromTheStart() {
        // Given
        MovableClock clock = new MovableClock(NOW);
        Cooldown cooldown = new Cooldown(clock, LENGTH);

        // When
        cooldown.start();

        // Then
        assertThat(cooldown.active()).isTrue();
        clock.moveTo(NOW.plus(LENGTH).minusMillis(1));
        assertThat(cooldown.active()).isTrue();
        clock.moveTo(NOW.plus(LENGTH));
        assertThat(cooldown.active()).isFalse();
    }

    @Test
    void restartingExtendsThePauseFromTheNewStart() {
        // Given
        MovableClock clock = new MovableClock(NOW);
        Cooldown cooldown = new Cooldown(clock, LENGTH);
        cooldown.start();

        // When
        clock.moveTo(NOW.plusSeconds(5));
        cooldown.start();

        // Then
        clock.moveTo(NOW.plusSeconds(12));
        assertThat(cooldown.active()).isTrue();
    }

    @Test
    void rejectsANegativeLength() {
        assertThatThrownBy(() -> new Cooldown(clockAt(NOW), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
