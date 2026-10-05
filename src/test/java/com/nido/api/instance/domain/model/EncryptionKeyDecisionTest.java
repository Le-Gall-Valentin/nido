package com.nido.api.instance.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionKeyDecisionTest {

    private static final String KEY = "the-key-of-this-installation-32chars";
    private static final String OTHER = "another-key-of-at-least-32-characters";
    private static final String FILE = "/data/secrets/encryption-key";
    private static final KeyFingerprint FINGERPRINT = KeyFingerprint.of(KEY);

    private static Optional<ProvidedKey> given(String key) {
        return Optional.of(new ProvidedKey(key, "NIDO_ENCRYPTION_SECRET"));
    }

    private static InstanceState state(KeyFingerprint fingerprint, boolean setupCompleted) {
        return new InstanceState(Optional.ofNullable(fingerprint), false, setupCompleted);
    }

    @Test
    void the_key_that_matches_the_fingerprint_is_used() {
        assertThat(EncryptionKeyDecision.decide(given(KEY), state(FINGERPRINT, true), FILE))
            .isEqualTo(new EncryptionKeyDecision.Use(KEY));
    }

    @Test
    void another_key_is_refused_and_named_by_where_it_came_from() {
        assertThat(EncryptionKeyDecision.decide(given(OTHER), state(FINGERPRINT, true), FILE))
            .isInstanceOfSatisfying(EncryptionKeyDecision.Refuse.class,
                refuse -> assertThat(refuse.reason()).contains("NIDO_ENCRYPTION_SECRET").contains("not the one"));
    }

    @Test
    void a_key_given_to_a_database_without_fingerprint_has_its_fingerprint_recorded() {
        assertThat(EncryptionKeyDecision.decide(given(KEY), state(null, true), FILE))
            .isEqualTo(new EncryptionKeyDecision.RecordFingerprint(KEY));
        assertThat(EncryptionKeyDecision.decide(given(KEY), state(null, false), FILE))
            .isEqualTo(new EncryptionKeyDecision.RecordFingerprint(KEY));
    }

    @Test
    void no_key_on_a_fresh_installation_means_one_is_generated() {
        assertThat(EncryptionKeyDecision.decide(Optional.empty(), state(null, false), FILE))
            .isInstanceOf(EncryptionKeyDecision.Generate.class);
    }

    @Test
    void no_key_where_data_was_encrypted_is_refused_and_says_where_to_put_it() {
        for (InstanceState state : new InstanceState[]{state(FINGERPRINT, true), state(null, true)}) {
            assertThat(EncryptionKeyDecision.decide(Optional.empty(), state, FILE))
                .isInstanceOfSatisfying(EncryptionKeyDecision.Refuse.class,
                    refuse -> assertThat(refuse.reason()).contains(FILE).contains("NIDO_ENCRYPTION_SECRET"));
        }
    }

    @Test
    void a_key_lost_before_the_setup_is_done_is_generated_again_since_nothing_was_encrypted_with_it() {
        assertThat(EncryptionKeyDecision.decide(Optional.empty(), state(FINGERPRINT, false), FILE))
            .isInstanceOf(EncryptionKeyDecision.Generate.class);
    }

    @Test
    void another_key_given_before_the_setup_is_done_replaces_the_fingerprint() {
        assertThat(EncryptionKeyDecision.decide(given(OTHER), state(FINGERPRINT, false), FILE))
            .isEqualTo(new EncryptionKeyDecision.RecordFingerprint(OTHER));
    }

    @Test
    void a_key_shorter_than_32_characters_is_refused() {
        assertThat(EncryptionKeyDecision.decide(given("short"), state(null, false), FILE))
            .isInstanceOfSatisfying(EncryptionKeyDecision.Refuse.class,
                refuse -> assertThat(refuse.reason()).contains("at least 32 characters"));
    }
}
