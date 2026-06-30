package com.synapsys.api.mfa.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceAdapterTest {

    private TotpServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TotpServiceAdapter();
    }

    @Test
    void generateSecret_returnsNonBlankString() {
        assertThat(adapter.generateSecret()).isNotBlank();
    }

    @Test
    void generateSecret_eachCallReturnsUniqueValue() {
        assertThat(adapter.generateSecret()).isNotEqualTo(adapter.generateSecret());
    }

    @Test
    void buildOtpauthUri_usesAlgorithmSha256() {
        String uri = adapter.buildOtpauthUri("SECRET", "user@test.com");

        assertThat(uri).contains("algorithm=SHA256");
    }

    @Test
    void buildOtpauthUri_hasCorrectStructure() {
        String uri = adapter.buildOtpauthUri("SECRET", "user@test.com");

        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("?secret=SECRET");
        assertThat(uri).contains("digits=6");
        assertThat(uri).contains("period=30");
    }

    @Test
    void buildOtpauthUri_encodesEmailSpacesAsPercent20() {
        String uri = adapter.buildOtpauthUri("SECRET", "first last@test.com");

        assertThat(uri).doesNotContain("+");
        assertThat(uri).contains("first%20last%40test.com");
    }

    @Test
    void buildOtpauthUri_encodesSpecialCharsInEmail() {
        String uri = adapter.buildOtpauthUri("SECRET", "user+tag@test.com");

        assertThat(uri).doesNotContain("user+tag");
        assertThat(uri).contains("user%2Btag%40test.com");
    }

    @Test
    void isValid_unknownSecret_returnsFalse() {
        assertThat(adapter.isValid("INVALIDSECRET", "000000")).isFalse();
    }
}