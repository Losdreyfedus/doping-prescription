package com.doping.prescription.auth.security;

import com.doping.prescription.auth.api.CurrentUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for springdoc (ADR 0004). The server URL is fixed so the generated document is identical on every
 * machine and can be compared with the committed {@code openapi.json}.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    static final String BEARER = "bearer";

    static {
        // CurrentUser comes from the token, not from the request; keep it out of the documented parameters.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser.class);
    }

    @Bean
    OpenAPI prescriptionOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Doping Prescription API").version("v1"))
                .servers(List.of(new Server().url("http://localhost:8080")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
