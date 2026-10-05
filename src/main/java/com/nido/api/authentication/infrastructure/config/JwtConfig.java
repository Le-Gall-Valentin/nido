package com.nido.api.authentication.infrastructure.config;

import com.nido.api.authentication.infrastructure.security.JwtKeyFactory;
import com.nido.api.infrastructure.config.DataDirectory;
import com.nido.api.infrastructure.config.NidoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    /**
     * NIDO_JWT_SECRET when the installation gives one; otherwise one generated in the data directory
     * and kept there. Losing that file signs everyone out, nothing worse: unlike the encryption key,
     * nothing stored depends on it.
     */
    @Bean
    public SecretKey jwtSecretKey(NidoProperties properties, DataDirectory dataDirectory) {
        return JwtKeyFactory.from(secret(properties.jwt() == null ? null : properties.jwt().secret(), dataDirectory));
    }

    static String secret(String configured, DataDirectory dataDirectory) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return dataDirectory.readOrCreateSecret("jwt-secret");
    }
}
