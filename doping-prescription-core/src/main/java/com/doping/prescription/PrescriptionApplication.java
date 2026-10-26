package com.doping.prescription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
// Spring Modulith's @ApplicationModuleListener handles events asynchronously after commit.
@EnableAsync
// Resubmission of incomplete event publications (shared.messaging.OutboxResubmission).
@EnableScheduling
// Spring instantiates the application class as a configuration bean, so it must keep a public constructor.
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class PrescriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrescriptionApplication.class, args);
    }
}
