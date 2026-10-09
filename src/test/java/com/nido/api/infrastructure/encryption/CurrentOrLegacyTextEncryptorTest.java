package com.nido.api.infrastructure.encryption;

import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentOrLegacyTextEncryptorTest {

    private static final String MASTER = "integration-test-encryption-secret-32chars!";
    private static final String SALT = "6e69646f2d6d61696c2d6f7574626f78";
    private static final CurrentOrLegacyTextEncryptor ENCRYPTOR = CurrentOrLegacyTextEncryptor.of(new EncryptionKey(MASTER), SALT);

    @Test
    void it_always_writes_with_the_current_key_behind_k2() {
        String stored = ENCRYPTOR.encrypt("s3cret");

        assertThat(stored).startsWith("k2:");
        assertThat(CurrentOrLegacyTextEncryptor.isCurrent(stored)).isTrue();
        assertThat(DataKeys.current(new EncryptionKey(MASTER), SALT).decrypt(stored.substring(3))).isEqualTo("s3cret");
        assertThat(ENCRYPTOR.decrypt(stored)).isEqualTo("s3cret");
    }

    @Test
    void what_earlier_versions_wrote_still_reads() {
        String legacy = LegacyKeys.writer(MASTER, SALT).encrypt("s3cret");

        assertThat(CurrentOrLegacyTextEncryptor.isCurrent(legacy)).isFalse();
        assertThat(ENCRYPTOR.decrypt(legacy)).isEqualTo("s3cret");
    }

    @Test
    void a_k2_value_that_does_not_open_is_refused_and_never_tried_with_the_legacy_key() {
        // The legacy key's ciphertext behind k2: — it would open with the legacy key, so a fallback would accept it.
        String forged = "k2:" + LegacyKeys.writer(MASTER, SALT).encrypt("s3cret");

        assertThatThrownBy(() -> ENCRYPTOR.decrypt(forged)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void another_master_key_opens_neither_format() {
        CurrentOrLegacyTextEncryptor other = CurrentOrLegacyTextEncryptor.of(new EncryptionKey("a-key-someone-typed-by-mistake-32-chars+"), SALT);

        assertThatThrownBy(() -> other.decrypt(ENCRYPTOR.encrypt("s3cret"))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> other.decrypt(LegacyKeys.writer(MASTER, SALT).encrypt("s3cret"))).isInstanceOf(RuntimeException.class);
    }
}
