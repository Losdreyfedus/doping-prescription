package com.doping.prescription;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.springframework.transaction.annotation.Transactional;

/** Layering rules that Spring Modulith's module verification does not cover (docs/kod-standartlari.md §2, §4). */
@AnalyzeClasses(packagesOf = PrescriptionApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    // Fully qualified: a pattern like "..auth.." would also match third-party packages (e.g. Kafka's security.auth).
    private static final String[] BUSINESS_MODULES = {
        "com.doping.prescription.auth..",
        "com.doping.prescription.content..",
        "com.doping.prescription.exam..",
        "com.doping.prescription.learner..",
        "com.doping.prescription.feed..",
        "com.doping.prescription.quiz..",
        "com.doping.prescription.points..",
        "com.doping.prescription.admin..",
        "com.doping.prescription.simulation.."
    };

    /**
     * Transaction boundaries besides application services. The demo seed loaders write their module's rows in one
     * transaction at startup; they live in {@code seed} packages next to the data they load. Remove the exception once
     * they delegate to an application service.
     */
    private static final String SEED_PACKAGES = "com.doping.prescription.*.seed..";

    /**
     * MapStruct mapper of the auth module that maps its user entity to the web DTOs. Remove the exception once the
     * mapping moves to the application layer (ADR 0008: controllers return the service's read model).
     */
    private static final String AUTH_WEB_MAPPER = "com.doping.prescription.auth.web.AuthWebMapper";

    /**
     * Its package-private transactional methods are called through the quiz service only. Remove the exception once
     * they are public.
     */
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
            .because("algorithms in domain packages are plain Java and unit-tested without a framework");

    @ArchTest
    static final ArchRule domainClassesAreFinal = classes()
            .that()
            .resideInAPackage("com.doping.prescription.*.domain..")
            .and()
            .areTopLevelClasses()
            .and()
            .areNotInterfaces()
            .and()
            .areNotEnums()
            .should()
            .haveModifier(JavaModifier.FINAL)
            .because(
                    "domain classes are immutable value objects and algorithms (docs/mimari.md §2); records are final");

    @ArchTest
    static final ArchRule sharedDoesNotDependOnBusinessModules = noClasses()
            .that()
            .resideInAPackage("com.doping.prescription.shared..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(BUSINESS_MODULES)
            .because("shared is an OPEN infrastructure module without business logic (ADR 0005)");

    @ArchTest
    static final ArchRule transactionsOnlyInApplicationServices = methods()
            .that()
            .areAnnotatedWith(Transactional.class)
            .should()
            .beDeclaredInClassesThat()
            .resideInAnyPackage("com.doping.prescription.*.application..", SEED_PACKAGES)
            .because("@Transactional belongs on application service methods (docs/kod-standartlari.md §2)");

    @ArchTest
    static final ArchRule transactionalMethodsArePublic = methods()
            .that()
            .areAnnotatedWith(Transactional.class)
            .should()
            .bePublic()
            .because("a transaction boundary is a visible, public service method (docs/kod-standartlari.md §2)");

    @ArchTest
    static final ArchRule noClassLevelTransactions = noClasses()
            .should()
            .beAnnotatedWith(Transactional.class)
            .because("each transaction boundary is a visible, public application service method");

    @ArchTest
    static final ArchRule webDoesNotReachPersistence = noClasses()
            .that()
            .resideInAPackage("com.doping.prescription.*.web..")
            .and()
            .doNotHaveFullyQualifiedName(AUTH_WEB_MAPPER)
            .and()
            .doNotHaveFullyQualifiedName(AUTH_WEB_MAPPER + "Impl")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.doping.prescription.*.persistence..")
            .because("controllers return the application service's read model; entities never leave the module");

    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule timeComesFromTheClockBean = noClasses()
            .that()
            .resideOutsideOfPackage("com.doping.prescription.shared.time..")
            .should()
            .callMethod(Instant.class, "now")
            .orShould()
            .callMethod(LocalDate.class, "now")
            .orShould()
            .callMethod(LocalDateTime.class, "now")
            .orShould()
            .callMethod(OffsetDateTime.class, "now")
            .orShould()
            .callMethod(ZonedDateTime.class, "now")
            .orShould()
            .callMethod(Clock.class, "systemUTC")
            .orShould()
            .callMethod(Clock.class, "systemDefaultZone")
            .orShould()
            .callMethod(System.class, "currentTimeMillis")
            .because("time comes from the Clock bean, so tests can fix it (docs/mimari.md §9)");
}
