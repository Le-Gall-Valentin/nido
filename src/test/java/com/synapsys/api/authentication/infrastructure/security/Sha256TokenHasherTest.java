package com.synapsys.api.authentication.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256TokenHasherTest {

    private final Sha256TokenHasher hasher = new Sha256TokenHasher();

    @Test
    void hash_returnsSha256HexString() {
        String result = hasher.hash("hello");

        assertThat(result).hasSize(64);
        assertThat(result).matches("[0-9a-f]+");
    }

    @Test
    void hash_isDeterministic() {
        String a = hasher.hash("same-input");
        String b = hasher.hash("same-input");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void hash_producesDifferentOutputsForDifferentInputs() {
        assertThat(hasher.hash("token-a")).isNotEqualTo(hasher.hash("token-b"));
    }

    @Test
    void hash_knownValue() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertThat(hasher.hash(""))
            .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}