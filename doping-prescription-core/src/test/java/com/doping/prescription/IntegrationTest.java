package com.doping.prescription;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Full application context against the shared Testcontainers infrastructure. All test classes using this annotation
 * without extra properties share one cached Spring context.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = IntegrationTest.JWT_SECRET_PROPERTY)
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {

    /** Test-only signing key; production reads PRESCRIPTION_JWT_SECRET. */
    String JWT_SECRET_PROPERTY = "prescription.auth.jwt-secret=test-only-secret-0123456789-abcdefghij";
}
