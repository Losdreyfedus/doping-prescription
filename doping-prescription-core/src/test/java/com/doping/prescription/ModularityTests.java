package com.doping.prescription;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    private static final ApplicationModules MODULES = ApplicationModules.of(PrescriptionApplication.class);

    @Test
    void detectsAllModulesDefinedInArchitectureDocument() {
        assertThat(MODULES.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
                .containsExactlyInAnyOrder(
                        "auth",
                        "content",
                        "exam",
                        "learner",
                        "feed",
                        "quiz",
                        "points",
                        "admin",
                        "simulation",
                        "shared");
    }

    @Test
    void modulesOnlyDependOnEachOthersPublicApi() {
        MODULES.verify();
    }

    /**
     * Regenerates docs/moduller on demand ({@code ./mvnw verify -Dmodulith.docs=true}). Not on every build: the
     * Documenter orders relations differently between runs, which would leave noise diffs in the working tree.
     */
    @Test
    @EnabledIfSystemProperty(named = "modulith.docs", matches = "true")
    void writesModuleDocumentation() {
        new Documenter(MODULES, Documenter.Options.defaults().withOutputFolder("../docs/moduller"))
                .writeDocumentation();
    }
}
