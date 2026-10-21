package com.bcp.workshop.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {
    private static final String SIGNING_SECRET = "unit-test-auth-token-signing-secret-2026";
    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");

    @Test
    void issuesSignedTokenWithRequiredIdentityClaims() {
        TokenService service = new TokenService(
                Optional.of(SIGNING_SECRET),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        TokenService.IssuedToken issued = service.issueToken("student", "angular-workshop");
        DecodedJWT token = JWT.decode(issued.value());
        Algorithm.HMAC256(SIGNING_SECRET).verify(token);

        assertFalse(issued.value().contains(SIGNING_SECRET));
        assertEquals("student", token.getSubject());
        assertEquals("angular-workshop", token.getClaim("client").asString());
        assertEquals(NOW.getEpochSecond(), token.getIssuedAt().toInstant().getEpochSecond());
        assertEquals(NOW.plusSeconds(3600).getEpochSecond(), token.getExpiresAt().toInstant().getEpochSecond());
        assertTrue(token.getClaim("iat").asLong() > 0);
        assertTrue(token.getClaim("exp").asLong() > token.getClaim("iat").asLong());
    }

    @Test
    void derivesExpiresInFromTheTokenExpirationInstant() {
        TokenService service = new TokenService(
                Optional.of(SIGNING_SECRET),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        TokenService.IssuedToken issued = service.issueToken("student", "angular-workshop");
        DecodedJWT token = JWT.decode(issued.value());
        long remainingSeconds = token.getExpiresAt().toInstant().getEpochSecond() - NOW.getEpochSecond();

        assertTrue(Math.abs(issued.expiresIn() - remainingSeconds) <= 1);
    }

    @Test
    void rejectsTokenAtItsExpirationInstant() {
        TokenService service = new TokenService(
                Optional.of(SIGNING_SECRET),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        TokenService.IssuedToken issued = service.issueToken("student", "angular-workshop");

        assertThrows(
                com.auth0.jwt.exceptions.JWTVerificationException.class,
                () -> service.validateToken(issued.value(), NOW.plusSeconds(3600))
        );
        assertThrows(
            com.auth0.jwt.exceptions.JWTVerificationException.class,
            () -> service.validateToken(issued.value(), NOW.plusSeconds(3601))
        );
    }

    @Test
    void doesNotIssueTokenWithoutConfiguredSigningSecret() {
        TokenService service = new TokenService(Optional.empty(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(IllegalStateException.class, () -> service.issueToken("student", "angular-workshop"));
    }

    @Test
    void rejectsTokenWithTamperedSignature() {
        TokenService service = new TokenService(
                Optional.of(SIGNING_SECRET),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        String token = service.issueToken("student", "angular-workshop").value();
        String[] segments = token.split("\\.");
        char replacement = segments[2].charAt(0) == 'A' ? 'B' : 'A';
        String tamperedToken = segments[0] + "." + segments[1] + "." + replacement + segments[2].substring(1);

        assertThrows(
                com.auth0.jwt.exceptions.JWTVerificationException.class,
                () -> service.validateToken(tamperedToken, NOW)
        );
    }
}