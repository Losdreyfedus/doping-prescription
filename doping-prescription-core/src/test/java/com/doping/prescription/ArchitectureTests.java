package com.doping.prescription;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Layering rules that Spring Modulith's module verification does not cover (docs/kod-standartlari.md §2, §4). */
@AnalyzeClasses(packagesOf = PrescriptionApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    private static final String[] BUSINESS_MODULES = {
        "..auth..",
        "..content..",
        "..exam..",
        "..learner..",
        "..feed..",
        "..quiz..",
        "..points..",
        "..admin..",
        "..simulation.."
    };

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "com.fasterxml.jackson..",
                    "tools.jackson..")
            .because("algorithms in domain packages are plain Java and unit-tested without a framework")
            // No domain classes exist yet; the rule must still be in place for the first one.
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule sharedDoesNotDependOnBusinessModules = noClasses()
            .that()
            .resideInAPackage("com.doping.prescription.shared..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(BUSINESS_MODULES)
            .because("shared is an OPEN infrastructure module without business logic (ADR 0005)");
}
