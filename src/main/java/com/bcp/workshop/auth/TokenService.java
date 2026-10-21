package com.bcp.workshop.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@ApplicationScoped
public class TokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 3600;

    private final Optional<String> signingSecret;
    private final Clock clock;

    @Inject
    public TokenService(@ConfigProperty(name = "auth.token.secret") Optional<String> signingSecret) {
        this(signingSecret, Clock.systemUTC());
    }

    TokenService(Optional<String> signingSecret, Clock clock) {
        this.signingSecret = signingSecret;
        this.clock = clock;
    }

    public IssuedToken issueToken(String username, String client) {
        String secret = requiredSigningSecret();
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(TOKEN_LIFETIME_SECONDS);
        String value = JWT.create()
                .withSubject(username)
                .withClaim("client", client)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.HMAC256(secret));
        int expiresIn = Math.toIntExact(Math.max(0, Duration.between(clock.instant(), expiresAt).getSeconds()));

        return new IssuedToken(value, expiresIn);
    }

    public DecodedJWT validateToken(String value) {
        return validateToken(value, clock.instant());
    }

    DecodedJWT validateToken(String value, Instant now) {
        DecodedJWT token = JWT.decode(value);
        Algorithm.HMAC256(requiredSigningSecret()).verify(token);
        Date expiresAt = token.getExpiresAt();
        if (expiresAt == null || !expiresAt.toInstant().isAfter(now)) {
            throw new JWTVerificationException("Token is expired or has no expiration claim");
        }
        return token;
    }

    private String requiredSigningSecret() {
        return signingSecret
                .filter(secret -> !secret.isBlank())
                .orElseThrow(() -> new IllegalStateException("AUTH_TOKEN_SECRET must be configured"));
    }

    record IssuedToken(String value, int expiresIn) {}
}