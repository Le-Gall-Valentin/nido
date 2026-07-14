package com.nido.api.authentication.infrastructure.config;

import com.nido.api.authentication.infrastructure.security.JwtKeyFactory;
import com.nido.api.infrastructure.config.NidoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(NidoProperties properties) {
        return JwtKeyFactory.from(properties.jwt().secret());
    }
}