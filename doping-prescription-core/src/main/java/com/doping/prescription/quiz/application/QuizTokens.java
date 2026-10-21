package com.doping.prescription.quiz.application;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Signed, short-lived quiz tokens (docs/api.md, quizToken): who may answer which question for which card, and the
 * shuffle mapping. Signed with its own key, not the session key.
 */
@Component
class QuizTokens {

    private static final String IMPRESSION = "imp";
    private static final String QUESTION = "mq";
    private static final String OPTIONS = "opt";

    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final QuizProperties properties;
    private final Clock clock;

    QuizTokens(QuizProperties properties, Clock clock) {
        SecretKey key = new SecretKeySpec(properties.tokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // expiry is checked below against the application clock, so it can answer 410 instead of 403
        this.decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        this.properties = properties;
        this.clock = clock;
    }

    String issue(QuizClaims claims) {
        Instant now = clock.instant();
        JwtClaimsSet set = JwtClaimsSet.builder()
                .id(claims.tokenId().toString())
                .subject(claims.userId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.tokenTtl()))
                .claim(IMPRESSION, claims.impressionId().toString())
                .claim(QUESTION, claims.microQuestionId())
                .claim(OPTIONS, claims.shownToStored())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, set)).getTokenValue();
    }

    /**
     * @throws QuizErrors.TokenInvalid if the signature or content is wrong
     * @throws QuizErrors.TokenExpired if the token is older than its lifetime
     */
    QuizClaims verify(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtException e) {
            throw new QuizErrors.TokenInvalid();
        }
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || !clock.instant().isBefore(expiresAt)) {
            throw new QuizErrors.TokenExpired();
        }
        Map<String, Object> options = jwt.getClaimAsMap(OPTIONS);
        Object question = jwt.getClaims().get(QUESTION);
        String impression = jwt.getClaimAsString(IMPRESSION);
        String tokenId = jwt.getId();
        String subject = jwt.getSubject();
        if (options == null
                || !(question instanceof Number number)
                || impression == null
                || tokenId == null
                || subject == null) {
            throw new QuizErrors.TokenInvalid();
        }
        try {
            Map<String, String> shownToStored = new LinkedHashMap<>();
            options.forEach((shown, stored) -> shownToStored.put(shown, String.valueOf(stored)));
            return new QuizClaims(
                    UUID.fromString(tokenId),
                    UUID.fromString(subject),
                    UUID.fromString(impression),
                    number.intValue(),
                    shownToStored);
        } catch (IllegalArgumentException e) {
            // malformed claims inside a validly signed token
            throw new QuizErrors.TokenInvalid();
        }
    }

    /**
     * @param tokenId       jti: one answer per token
     * @param shownToStored shown option letter → stored option letter
     */
    record QuizClaims(
            UUID tokenId, UUID userId, UUID impressionId, Integer microQuestionId, Map<String, String> shownToStored) {

        QuizClaims {
            shownToStored = Map.copyOf(shownToStored);
        }
    }
}
