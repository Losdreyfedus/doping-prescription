package com.doping.prescription.quiz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.doping.prescription.quiz.application.QuizTokens.QuizClaims;
import com.doping.prescription.shared.error.DomainException;
import com.doping.prescription.shared.error.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class QuizTokensTest {

    private static final Instant NOW = Instant.parse("2026-10-20T07:00:00Z");
    private static final String SECRET = "test-only-quiz-secret-0123456789-abcdefghij";
    private static final QuizProperties PROPERTIES =
            new QuizProperties(0.7, 60, Duration.ofMinutes(5), 2000, 30, SECRET);
    private static final QuizClaims CLAIMS =
            new QuizClaims(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 7, Map.of("A", "C", "B", "A"));

    private final QuizTokens tokens = new QuizTokens(PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void anIssuedTokenVerifiesToItsClaims() {
        assertThat(tokens.verify(tokens.issue(CLAIMS))).isEqualTo(CLAIMS);
    }

    @Test
    void aTokenPastItsLifetimeIsExpired() {
        // Given
        String token = tokens.issue(CLAIMS);
        QuizTokens later = new QuizTokens(PROPERTIES, Clock.fixed(NOW.plus(Duration.ofMinutes(5)), ZoneOffset.UTC));

        // When / Then
        assertThatThrownBy(() -> later.verify(token)).satisfies(failure -> assertCode(failure, ErrorCode.QUIZ_EXPIRED));
    }

    @Test
    void aTokenForAnotherAudienceIsInvalidEvenWithTheRightKey() {
        // Given: the quiz claims signed with the quiz key, but issued for another purpose
        String token = signed(List.of("session"));

        // When / Then
        assertThatThrownBy(() -> tokens.verify(token))
                .satisfies(failure -> assertCode(failure, ErrorCode.QUIZ_TOKEN_INVALID));
    }

    @Test
    void aTokenWithoutAudienceIsInvalid() {
        String token = signed(List.of());

        assertThatThrownBy(() -> tokens.verify(token))
                .satisfies(failure -> assertCode(failure, ErrorCode.QUIZ_TOKEN_INVALID));
    }

    @Test
    void theQuizKeyMustDifferFromTheSessionKey() {
        MockEnvironment sameKey =
                new MockEnvironment().withProperty(DistinctTokenSecrets.SESSION_SECRET_PROPERTY, SECRET);
        MockEnvironment ownKey =
                new MockEnvironment().withProperty(DistinctTokenSecrets.SESSION_SECRET_PROPERTY, SECRET + "-session");

        assertThatThrownBy(() -> new DistinctTokenSecrets(PROPERTIES, sameKey).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new DistinctTokenSecrets(PROPERTIES, ownKey).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    private static String signed(List<String> audience) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .id(CLAIMS.tokenId().toString())
                .subject(CLAIMS.userId().toString())
                .issuedAt(NOW)
                .expiresAt(NOW.plus(Duration.ofMinutes(5)))
                .claim(QuizTokens.IMPRESSION, CLAIMS.impressionId().toString())
                .claim(QuizTokens.QUESTION, CLAIMS.microQuestionId())
                .claim(QuizTokens.OPTIONS, CLAIMS.shownToStored());
        if (!audience.isEmpty()) {
            claims.audience(audience);
        }
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

    private static void assertCode(Throwable failure, ErrorCode code) {
        assertThat(failure).isInstanceOf(DomainException.class);
        assertThat(((DomainException) failure).code()).isEqualTo(code);
    }
}
