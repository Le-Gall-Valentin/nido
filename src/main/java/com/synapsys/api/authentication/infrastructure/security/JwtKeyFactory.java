package com.synapsys.api.authentication.infrastructure.security;

import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public final class JwtKeyFactory {
    private JwtKeyFactory() {}

    public static SecretKey from(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("synapsys.jwt.secret must be configured");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "synapsys.jwt.secret must be at least 32 bytes long (got " + keyBytes.length + ")"
            );
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}