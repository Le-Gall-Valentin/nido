package com.nido.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "nido.jwt.secret=integration-test-secret-at-least-32-chars!",
    "nido.jwt.expiry-minutes=15",
    "nido.refresh-token.expiry-days=30",
    "nido.cookie.secure=false",
    "nido.seed.username=it-admin",
    "nido.seed.email=it-admin@test.local",
    "nido.seed.password=integration-test-seed-password",
    "nido.cors.allowed-origins=",
    "nido.encryption.secret=integration-test-encryption-secret-32chars!",
    "spring.jpa.hibernate.ddl-auto=none"
})
public @interface IntegrationTestConfig {}