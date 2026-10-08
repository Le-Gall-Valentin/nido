package com.nido.api.mfa.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRandomMailCodeAdapterTest {

    @Test
    void a_small_draw_keeps_its_leading_zeros() {
        RandomGenerator fixed = new RandomGenerator() {
            @Override public long nextLong() { return 0; }
            @Override public int nextInt(int bound) { return 4213; }
        };

        assertThat(new SecureRandomMailCodeAdapter(fixed).newCode()).isEqualTo("004213");
    }

    @Test
    void every_code_is_six_digits() {
        SecureRandomMailCodeAdapter codes = new SecureRandomMailCodeAdapter();
        for (int i = 0; i < 1_000; i++) {
            assertThat(codes.newCode()).matches("\\d{6}");
        }
    }
}
