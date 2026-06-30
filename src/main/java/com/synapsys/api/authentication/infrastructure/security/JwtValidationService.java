package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.shared.model.Role;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

@Component
public class JwtValidationService {

    private final SecretKey key;
    private final String issuer;
    private final String audience;

    public JwtValidationService(SecretKey jwtSecretKey, SynapsysProperties properties) {
        this.key = jwtSecretKey;
        this.issuer = properties.jwt().issuer();
        this.audience = properties.jwt().audience();
    }

    public UserClaims validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!issuer.equals(claims.getIssuer())) {
                throw new JwtException("Invalid issuer");
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
                throw new JwtException("Invalid audience");
            }
            String subject = claims.getSubject();
            if (subject == null) throw new JwtException("Missing subject claim");
            UUID userId = UUID.fromString(subject);

            String roleStr = claims.get("role", String.class);
            if (roleStr == null) throw new JwtException("Missing role claim");
            Role role = Role.valueOf(roleStr);
            String email = claims.get("email", String.class);
            return new UserClaims(userId, role, email);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}