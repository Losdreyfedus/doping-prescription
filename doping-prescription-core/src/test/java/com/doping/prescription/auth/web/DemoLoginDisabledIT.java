package com.doping.prescription.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.auth.application.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Outside the demo profile the passwordless login does not exist: nobody can become a user by naming them. */
@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = AuthProperties.DEMO_LOGIN_ENABLED + "=false")
class DemoLoginDisabledIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void demoLoginAnswersNotFound() {
        // When
        MvcTestResult result = mvc.post()
                .uri("/api/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"admin\"}")
                .exchange();

        // Then
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
        assertThat(result).bodyText().doesNotContain("token");
    }

    @Test
    void demoUsersAnswerNotFound() {
        // When
        MvcTestResult result = mvc.get().uri("/api/auth/demo-users").exchange();

        // Then
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
    }
}
