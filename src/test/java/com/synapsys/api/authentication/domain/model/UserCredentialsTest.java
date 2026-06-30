package com.synapsys.api.authentication.domain.model;

import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserCredentialsTest {

    @Test
    void toString_doesNotExpose_passwordHash() {
        UserCredentials creds = new UserCredentials(
            UUID.randomUUID(), "alice", "alice@test.com",
            "$2a$12$very-sensitive-hash", true, Role.USER
        , Instant.now());

        assertThat(creds.toString()).doesNotContain("very-sensitive-hash");
    }

    @Test
    void toString_containsUsernameAndRole() {
        UserCredentials creds = new UserCredentials(
            UUID.randomUUID(), "alice", "alice@test.com",
            "$2a$12$hash", true, Role.USER
        , Instant.now());

        assertThat(creds.toString()).contains("alice");
    }
}