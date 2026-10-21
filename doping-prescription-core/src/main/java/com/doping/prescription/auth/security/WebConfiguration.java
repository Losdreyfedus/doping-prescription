package com.doping.prescription.auth.security;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the {@code CurrentUser} parameter resolver for all controllers. */
@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserResolver;

    WebConfiguration(CurrentUserArgumentResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }
}
