package com.nido.api.mfa.infrastructure.security;

import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacMailCodeHasherAdapterTest {

    private final HmacMailCodeHasherAdapter hasher = new HmacMailCodeHasherAdapter(new EncryptionKey("installation-key"));

    @Test
    void the_hash_depends_on_the_code_and_on_what_it_is_bound_to() {
        String hash = hasher.codeHash("challenge-1", "004213");

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(hasher.codeHash("challenge-1", "004213")).isEqualTo(hash);
        assertThat(hasher.codeHash("challenge-2", "004213")).isNotEqualTo(hash);
        assertThat(hasher.codeHash("challenge-1", "004214")).isNotEqualTo(hash);
    }

    @Test
    void a_code_matches_only_under_its_own_binding() {
        String hash = hasher.codeHash("challenge-1", "004213");

        assertThat(hasher.matches(hash, "challenge-1", "004213")).isTrue();
        assertThat(hasher.matches(hash, "challenge-2", "004213")).isFalse();
        assertThat(hasher.matches(hash, "challenge-1", "4213")).isFalse();
    }

    @Test
    void the_binding_hash_tells_two_bindings_apart_without_keeping_either() {
        assertThat(hasher.bindingHash("challenge-1")).hasSize(64)
            .isNotEqualTo(hasher.bindingHash("challenge-2"))
            .doesNotContain("challenge");
    }

    @Test
    void without_the_installation_key_neither_hash_can_be_recomputed() {
        // The binding is often the account id, which the database holds: what it holds must not be enough to
        // test the million possible codes against a stored hash.
        HmacMailCodeHasherAdapter otherInstallation = new HmacMailCodeHasherAdapter(new EncryptionKey("another-key"));

        assertThat(otherInstallation.codeHash("challenge-1", "004213")).isNotEqualTo(hasher.codeHash("challenge-1", "004213"));
        assertThat(otherInstallation.bindingHash("challenge-1")).isNotEqualTo(hasher.bindingHash("challenge-1"));
    }
}
