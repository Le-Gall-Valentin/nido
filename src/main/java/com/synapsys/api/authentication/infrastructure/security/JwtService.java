package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.domain.port.out.AccessTokenPort;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService implements AccessTokenPort {

    private final SecretKey key;
    private final int expiryMinutes;
    private final String issuer;
    private final String audience;

    public JwtService(SecretKey jwtSecretKey, SynapsysProperties properties) {
        this.key = jwtSecretKey;
        this.expiryMinutes = properties.jwt().expiryMinutes();
        this.issuer = properties.jwt().issuer();
        this.audience = properties.jwt().audience();
    }

    @Override
    public String generate(UserCredentials user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(issuer)
            .audience().add(audience).and()
            .subject(user.id().toString())
            .claim("role", user.role().name())
            .claim("email", user.email())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expiryMinutes * 60L)))
            .signWith(key)
            .compact();
    }
}