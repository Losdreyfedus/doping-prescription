package com.doping.prescription;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The OpenAPI document generated from the code must equal the committed {@code doping-prescription-ui/openapi.json}
 * (ADR 0004), so every API change shows up as a reviewable diff and in the UI's generated types. Regenerate with
 * {@code ./mvnw verify -Dopenapi.update=true}.
 */
@IntegrationTest
@AutoConfigureMockMvc
class OpenApiContractIT {

    private static final Path CONTRACT = Path.of("..", "doping-prescription-ui", "openapi.json");
    private static final String UPDATE_PROPERTY = "openapi.update";
    private static final JsonMapper JSON =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    @Autowired
    private MockMvcTester mvc;

    @Test
    void generatedDocumentMatchesCommittedContract() throws IOException {
        MvcTestResult result = mvc.get().uri("/v3/api-docs").exchange();
        assertThat(result).hasStatusOk();
        JsonNode generated = JSON.readTree(result.getResponse().getContentAsByteArray());

        if (Boolean.getBoolean(UPDATE_PROPERTY) || !Files.exists(CONTRACT)) {
            Files.writeString(CONTRACT, JSON.writeValueAsString(generated) + "\n", StandardCharsets.UTF_8);
        }

        JsonNode committed = JSON.readTree(Files.readString(CONTRACT, StandardCharsets.UTF_8));
        assertThat(generated)
                .as("openapi.json is out of date; run ./mvnw verify -D%s=true and commit the file", UPDATE_PROPERTY)
                .isEqualTo(committed);
    }
}
