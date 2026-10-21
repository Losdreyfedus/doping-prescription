package com.doping.prescription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
// Spring Modulith's @ApplicationModuleListener handles events asynchronously after commit.
@EnableAsync
// Spring instantiates the application class as a configuration bean, so it must keep a public constructor.
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class PrescriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrescriptionApplication.class, args);
    }
}
