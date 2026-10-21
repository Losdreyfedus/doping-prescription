package com.doping.prescription.auth.security;

import com.doping.prescription.auth.api.CurrentUser;
import com.doping.prescription.auth.application.TokenService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Fills {@link CurrentUser} controller parameters from the validated JWT, so no module touches Spring Security. */
@Component
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String ADMIN = "ADMIN";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public CurrentUser resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer container,
            NativeWebRequest request,
            WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            // Security rules guarantee a token on every endpoint that asks for the current user.
            throw new IllegalStateException("No authenticated user for " + parameter.getExecutable());
        }
        var token = jwtAuthentication.getToken();
        // Tokens are issued by TokenService with all three claims; a token without them is a programming error.
        String subject = Objects.requireNonNull(token.getSubject(), "token without subject");
        String slug = Objects.requireNonNull(token.getClaimAsString(TokenService.CLAIM_SLUG), "token without slug");
        return new CurrentUser(
                UUID.fromString(subject), slug, ADMIN.equals(token.getClaimAsString(TokenService.CLAIM_ROLE)));
    }
}
