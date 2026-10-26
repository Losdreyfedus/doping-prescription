package com.doping.prescription;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Full application context against PostgreSQL only (Redis and Kafka unreachable, see
 * {@link TestcontainersConfiguration}). All test classes using this annotation without extra properties share one
 * cached Spring context.
 *
 * <p>Event externalization is off here: without a broker every externalized event would fail to reach Kafka and log
 * an error. In-process listeners (e.g. content tagging) and the outbox registry for them work as usual; tests of the
 * Kafka path use {@link InfrastructureContainersConfiguration}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {IntegrationTest.JWT_SECRET_PROPERTY, IntegrationTest.NO_EXTERNALIZATION_PROPERTY})
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {

    /** Test-only signing key; production reads PRESCRIPTION_JWT_SECRET. */
    String JWT_SECRET_PROPERTY = "prescription.auth.jwt-secret=test-only-secret-0123456789-abcdefghij";

    /** Set as a test property, not dynamically: auto-configuration conditions read it before dynamic properties exist. */
    String NO_EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false";
}
