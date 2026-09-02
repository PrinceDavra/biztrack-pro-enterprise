package com.biztrackpro.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Issues and validates JWT bearer tokens (Auth0 java-jwt / HMAC-256).
 * The signing secret comes from the JWT_SECRET environment variable in production.
 */
@ApplicationScoped
public class JwtUtil {

    private static final String ISSUER = "biztrack-pro";
    private static final long EXPIRY_DAYS = 7;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            // Development fallback. Always set JWT_SECRET in production.
            secret = "biztrack-pro-dev-secret-change-me-in-production-0123456789";
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
    }

    public String generateToken(Long tenantId, String email) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(tenantId))
                .withClaim("email", email)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(EXPIRY_DAYS, ChronoUnit.DAYS))
                .sign(algorithm);
    }

    /** Verifies signature + issuer + expiry, throwing on any problem. */
    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }

    public Long extractTenantId(DecodedJWT jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public String extractEmail(DecodedJWT jwt) {
        return jwt.getClaim("email").asString();
    }
}
