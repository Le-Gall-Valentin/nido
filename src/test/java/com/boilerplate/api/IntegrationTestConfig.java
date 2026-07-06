package com.boilerplate.api;

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
    "boilerplate.jwt.secret=integration-test-secret-at-least-32-chars!",
    "boilerplate.jwt.expiry-minutes=15",
    "boilerplate.refresh-token.expiry-days=30",
    "boilerplate.cookie.secure=false",
    "boilerplate.seed.username=it-admin",
    "boilerplate.seed.email=it-admin@test.local",
    "boilerplate.seed.password=integration-test-seed-password",
    "boilerplate.cors.allowed-origins=",
    "boilerplate.encryption.secret=integration-test-encryption-secret-32chars!",
    "spring.jpa.hibernate.ddl-auto=none"
})
public @interface IntegrationTestConfig {}