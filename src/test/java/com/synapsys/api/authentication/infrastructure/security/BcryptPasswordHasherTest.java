package com.synapsys.api.authentication.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher =
        new BcryptPasswordHasher(new BCryptPasswordEncoder(4));

    @Test
    void hash_returnsBcryptHash() {
        String hash = hasher.hash("my-password");
        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void matches_correctPassword_returnsTrue() {
        String hash = hasher.hash("correct");
        assertThat(hasher.matches("correct", hash)).isTrue();
    }

    @Test
    void matches_wrongPassword_returnsFalse() {
        String hash = hasher.hash("correct");
        assertThat(hasher.matches("wrong", hash)).isFalse();
    }
}