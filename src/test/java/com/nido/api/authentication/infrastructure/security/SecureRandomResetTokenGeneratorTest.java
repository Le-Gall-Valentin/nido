package com.nido.api.authentication.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRandomResetTokenGeneratorTest {

    @Test
    void draws_256_bits_that_fit_in_a_url_fragment() {
        String token = new SecureRandomResetTokenGenerator().newToken();

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void never_draws_the_same_twice() {
        SecureRandomResetTokenGenerator generator = new SecureRandomResetTokenGenerator();
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            tokens.add(generator.newToken());
        }

        assertThat(tokens).hasSize(1_000);
    }
}
