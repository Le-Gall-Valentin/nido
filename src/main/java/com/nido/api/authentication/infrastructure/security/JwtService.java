package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.AccessTokenPort;
import com.nido.api.authentication.domain.port.out.SessionSettingsPort;
import com.nido.api.infrastructure.config.NidoProperties;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService implements AccessTokenPort {

    private final SecretKey key;
    private final String issuer;
    private final String audience;
    private final SessionSettingsPort sessionSettings;

    public JwtService(SecretKey jwtSecretKey, NidoProperties properties, SessionSettingsPort sessionSettings) {
        this.key = jwtSecretKey;
        this.issuer = properties.jwt().issuer();
        this.audience = properties.jwt().audience();
        this.sessionSettings = sessionSettings;
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
            .expiration(Date.from(now.plusSeconds(sessionSettings.accessTokenMinutes() * 60L)))
            .signWith(key)
            .compact();
    }
}