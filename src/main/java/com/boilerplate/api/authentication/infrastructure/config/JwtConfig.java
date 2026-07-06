package com.boilerplate.api.authentication.infrastructure.config;

import com.boilerplate.api.authentication.infrastructure.security.JwtKeyFactory;
import com.boilerplate.api.infrastructure.config.BoilerplateProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(BoilerplateProperties properties) {
        return JwtKeyFactory.from(properties.jwt().secret());
    }
}