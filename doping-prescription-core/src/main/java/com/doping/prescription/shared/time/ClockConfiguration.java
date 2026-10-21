package com.doping.prescription.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Single source of "now" for the application; tests replace it with a fixed clock. */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock(TimeProperties properties) {
        return Clock.system(properties.timezone());
    }
}
