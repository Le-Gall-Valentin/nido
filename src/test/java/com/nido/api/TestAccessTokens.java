package com.nido.api;

import com.nido.api.shared.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Access-token cookies for integration tests: a request carrying one is that account, signed in, without
 * going through the login. Signed with the secret of {@link IntegrationTestConfig}.
 */
public final class TestAccessTokens {

    // Exact value of IntegrationTestConfig: any difference turns every request into a 401.
    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    private TestAccessTokens() {}

    public static Cookie cookieFor(UUID userId) {
        return cookieFor(userId, Role.USER);
    }

    public static Cookie cookieFor(UUID userId, Role role) {
        Instant now = Instant.now();
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", role.name())
            .claim("email", userId + "@test.local")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}
