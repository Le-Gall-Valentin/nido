package com.nido.api.mfa.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailCodeDigestTest {

    @Test
    void the_digest_depends_on_the_code_and_on_what_it_is_bound_to() {
        String digest = MailCodeDigest.of("challenge-1", "004213");

        assertThat(digest).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(MailCodeDigest.of("challenge-1", "004213")).isEqualTo(digest);
        assertThat(MailCodeDigest.of("challenge-2", "004213")).isNotEqualTo(digest);
        assertThat(MailCodeDigest.of("challenge-1", "004214")).isNotEqualTo(digest);
    }

    @Test
    void a_code_matches_only_under_its_own_binding() {
        String digest = MailCodeDigest.of("challenge-1", "004213");

        assertThat(MailCodeDigest.matches(digest, "challenge-1", "004213")).isTrue();
        assertThat(MailCodeDigest.matches(digest, "challenge-2", "004213")).isFalse();
        assertThat(MailCodeDigest.matches(digest, "challenge-1", "4213")).isFalse();
    }

    @Test
    void the_binding_hash_tells_two_bindings_apart_without_keeping_either() {
        assertThat(MailCodeDigest.bindingHash("challenge-1")).hasSize(64)
            .isNotEqualTo(MailCodeDigest.bindingHash("challenge-2"))
            .doesNotContain("challenge");
    }
}
