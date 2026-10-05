package com.nido.api.instance.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyFingerprintTest {

    private static final byte[] SALT = new byte[16];
    private static final int FAST = 1_000;

    @Test
    void the_production_cost_is_600_000_iterations() {
        assertThat(KeyFingerprint.ITERATIONS).isEqualTo(600_000);
    }

    @Test
    void it_recognises_the_key_it_was_made_from_and_no_other() {
        KeyFingerprint fingerprint = KeyFingerprint.of("the-key-of-this-installation-32chars", SALT, FAST);

        assertThat(fingerprint.matches("the-key-of-this-installation-32chars", FAST)).isTrue();
        assertThat(fingerprint.matches("another-key-of-at-least-32-characters", FAST)).isFalse();
    }

    @Test
    void a_fresh_salt_makes_a_different_fingerprint_of_the_same_key() {
        KeyFingerprint first = KeyFingerprint.of("the-key-of-this-installation-32chars");
        KeyFingerprint second = KeyFingerprint.of("the-key-of-this-installation-32chars");

        assertThat(first.salt()).hasSize(16).isNotEqualTo(second.salt());
        assertThat(first.hash()).hasSize(32).isNotEqualTo(second.hash());
        assertThat(first.matches("the-key-of-this-installation-32chars")).isTrue();
    }

    @Test
    void it_never_shows_its_bytes() {
        assertThat(KeyFingerprint.of("k".repeat(32), SALT, FAST).toString()).isEqualTo("KeyFingerprint[***]");
    }
}
