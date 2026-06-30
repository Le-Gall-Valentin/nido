package com.synapsys.api;

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
    "synapsys.jwt.secret=integration-test-secret-at-least-32-chars!",
    "synapsys.jwt.expiry-minutes=15",
    "synapsys.refresh-token.expiry-days=30",
    "synapsys.cookie.secure=false",
    "synapsys.seed.username=it-admin",
    "synapsys.seed.email=it-admin@test.local",
    "synapsys.seed.password=integration-test-seed-password",
    "synapsys.cors.allowed-origins=",
    "synapsys.encryption.secret=integration-test-encryption-secret-32chars!",
    "spring.jpa.hibernate.ddl-auto=none"
})
public @interface IntegrationTestConfig {}