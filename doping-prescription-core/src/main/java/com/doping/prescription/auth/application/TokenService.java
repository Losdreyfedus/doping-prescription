package com.doping.prescription.auth.application;

import com.doping.prescription.auth.persistence.UserAccount;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues the stateless session token (HS256 JWT, docs/mimari.md §6). */
@Service
public class TokenService {

    public static final String CLAIM_SLUG = "slug";
    public static final String CLAIM_ROLE = "role";

    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;

    TokenService(JwtEncoder encoder, AuthProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(UserAccount user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_SLUG, user.getSlug())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(properties.tokenTtl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
