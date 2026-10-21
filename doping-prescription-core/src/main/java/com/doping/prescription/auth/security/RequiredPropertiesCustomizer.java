package com.doping.prescription.auth.security;

import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Our DTOs are records whose components are always present unless explicitly nullable, but springdoc marks every
 * property optional. That would make every field optional in the generated TypeScript types. This marks all
 * properties required except those annotated {@code @Schema(nullable = true)}.
 */
@Component
class RequiredPropertiesCustomizer implements OpenApiCustomizer {

    @Override
    public void customise(io.swagger.v3.oas.models.OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        openApi.getComponents().getSchemas().values().forEach(RequiredPropertiesCustomizer::requireNonNullable);
    }

    private static void requireNonNullable(Schema<?> schema) {
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null) {
            return;
        }
        List<String> required = properties.entrySet().stream()
                .filter(property -> !Boolean.TRUE.equals(property.getValue().getNullable()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        schema.setRequired(required.isEmpty() ? null : required);
    }
}
