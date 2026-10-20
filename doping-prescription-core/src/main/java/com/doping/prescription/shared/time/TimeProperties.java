package com.doping.prescription.shared.time;

import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Time zone used for calendar-day calculations such as the daily points cap (docs/mimari.md §9).
 *
 * @param timezone zone that defines where a "day" starts and ends
 */
// TODO(spec): docs/algoritma.md §9 describes one PrescriptionProperties record with nested groups.
// Each module will bind its own group under "prescription.*" to keep config inside module boundaries.
@Validated
@ConfigurationProperties(prefix = "prescription")
public record TimeProperties(@NotNull ZoneId timezone) {}
