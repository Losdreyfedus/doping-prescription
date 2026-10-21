package com.doping.prescription.admin.demo;

import com.doping.prescription.shared.api.DemoDataLoader;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Loads the demo data at startup of the {@code demo} profile. It only orders the modules' loaders (auth, content,
 * exam); each loader seeds its own tables and is idempotent, so restarting the application never duplicates data.
 */
@Component
@Profile("demo")
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final List<DemoDataLoader> loaders;

    /** @param loaders sorted by Spring according to their {@code @Order} */
    DemoDataSeeder(List<DemoDataLoader> loaders) {
        this.loaders = List.copyOf(loaders);
    }

    @Override
    public void run(ApplicationArguments args) {
        for (DemoDataLoader loader : loaders) {
            loader.load();
            log.info("Demo data loaded module={}", loader.name());
        }
    }
}
