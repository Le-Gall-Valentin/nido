package com.nido.api.infrastructure.sealing;

import com.nido.api.infrastructure.encryption.DataKeys;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnKeyEncryptorsTest {

    private static final String MASTER = "integration-test-encryption-secret-32chars!";
    private static final String SALT = "6e69646f2d6d61696c2d6f7574626f78";

    @Test
    void a_value_is_written_with_the_current_key_of_the_salt() {
        TextEncryptor encryptor = new OwnKeyEncryptors(new EncryptionKey(MASTER), FixedLegacyFormats.closed()).forSalt(SALT);

        String stored = encryptor.encrypt("s3cret");

        assertThat(stored).startsWith("k2:");
        assertThat(DataKeys.current(new EncryptionKey(MASTER), SALT).decrypt(stored.substring(3))).isEqualTo("s3cret");
    }

    @Test
    void what_earlier_versions_wrote_reads_until_earlier_formats_close_which_is_asked_at_each_read() {
        AtomicBoolean closed = new AtomicBoolean();
        LegacyFormats formats = new LegacyFormats() {
            @Override
            public boolean closed() {
                return closed.get();
            }

            @Override
            public void closeForGood() {
                throw new AssertionError("an encryptor closes nothing");
            }
        };
        TextEncryptor encryptor = new OwnKeyEncryptors(new EncryptionKey(MASTER), formats).forSalt(SALT);
        String legacy = LegacyKeys.writer(MASTER, SALT).encrypt("s3cret");

        assertThat(encryptor.decrypt(legacy)).isEqualTo("s3cret");

        closed.set(true);

        assertThatThrownBy(() -> encryptor.decrypt(legacy)).isInstanceOf(IllegalStateException.class);
    }
}
