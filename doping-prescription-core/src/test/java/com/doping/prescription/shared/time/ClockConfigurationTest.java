package com.doping.prescription.shared.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ClockConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void clockUsesConfiguredTimezone() {
        contextRunner
                .withPropertyValues("prescription.timezone=Europe/Istanbul")
                .run(context ->
                        assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Europe/Istanbul")));
    }

    @Test
    void failsToStartWithoutTimezone() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TimeProperties.class)
    @Import(ClockConfiguration.class)
    static class TestConfig {}
}
