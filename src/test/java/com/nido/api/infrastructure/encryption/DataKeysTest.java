package com.nido.api.infrastructure.encryption;

import com.nido.api.shared.security.EncryptionKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataKeysTest {

    private static final String MASTER = "integration-test-encryption-secret-32chars!";
    private static final EncryptionKey KEY = new EncryptionKey(MASTER);
    private static final String SPACE_SALT = "0123456789abcdef0123456789abcdef";

    /**
     * Written by Encryptors.delux on Spring Security 7.0.7 — nido 0.15.2 — on 2026-10-09, with the master key above:
     * the legacy key has to open them byte for byte, or the upgrade would lose data.
     */
    @Test
    void the_legacy_key_opens_what_encryptors_delux_wrote() {
        assertThat(DataKeys.legacy(KEY, SPACE_SALT).decrypt(
            "0f8b56cf6b6e5dbfcce9338d297122b03bac65b16d7a9413285eb5bf4cf49f4e940c847bbbfaf08bdc5561cc3689c341cfb0"))
            .isEqualTo("Loyer octobre 🏡");
        assertThat(DataKeys.legacy(KEY, "00000000000040008000000000000001").decrypt(
            "06f825abf7343c8ab0d30e90d71ae9c756c2f86afbf68d686246f1ea4c1b087272d7199be289a5d1a9efdafeaf2e7742"))
            .isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(DataKeys.legacy(KEY, "6e69646f2d696e7374616e63652d73657474696e6773").decrypt(
            "a1ae372786040edb5a58c7d2f68653c3edc5bc3d04cce1ffb75a6ba3aceb8de78c52b2ed457f"))
            .isEqualTo("s3cret");
        assertThat(DataKeys.legacy(KEY, "6e69646f2d6d61696c2d6f7574626f78").decrypt(
            "82a4ddeb81741cb86e002bc544536a9abe0d1be470c9a5062bd7db562116f80eef8a8b9fa13de8ef3cdf9ea7784a92169c22173132eefad612c71df38b1f"))
            .isEqualTo("{\"address\":\"jane@example.com\"}");
        // A value sealed by 0.14.0 to 0.15.2: v2: and an envelope.
        assertThat(DataKeys.legacy(KEY, SPACE_SALT).decrypt(
            "77d264e59d9496536ebef0782f167dc5898f6a4e0a590d9db218d140bce9cf0f229ab9fdcf7f3eefed45cb40e4d98ad24abfd90622f042b7"
            + "9836df762175bd11cff7ebda42d59c57e66cef45c7dcb9ca981e615ff745be3fe7bd01e0a77908394ea79880451a75ffb4f11e81b748139b52"
            + "ecfd71f58cd7ec680e06240db259908edbe8fc4d983c0dc1070a9dcda0f29aff00be5f98"))
            .isEqualTo("finance_transactions.amount_encrypted:550e8400-e29b-41d4-a716-446655440000|000000006|850.00"
                + "#".repeat(26));
    }

    @Test
    void the_current_key_gives_back_what_it_encrypted_and_never_writes_the_same_twice() {
        TextEncryptor current = DataKeys.current(KEY, SPACE_SALT);

        String once = current.encrypt("Crème fraîche 🥛");

        assertThat(current.decrypt(once)).isEqualTo("Crème fraîche 🥛");
        assertThat(current.encrypt("Crème fraîche 🥛")).isNotEqualTo(once);
        assertThat(once).matches("[0-9a-f]+");
    }

    @Test
    void neither_key_opens_what_the_other_wrote() {
        String current = DataKeys.current(KEY, SPACE_SALT).encrypt("Loyer");
        String legacy = LegacyKeys.writer(MASTER, SPACE_SALT).encrypt("Loyer");

        assertThatThrownBy(() -> DataKeys.legacy(KEY, SPACE_SALT).decrypt(current)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> DataKeys.current(KEY, SPACE_SALT).decrypt(legacy)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void a_ciphertext_altered_in_one_byte_is_refused() {
        String stored = DataKeys.current(KEY, SPACE_SALT).encrypt("850.00");
        String altered = stored.substring(0, stored.length() - 1) + (stored.endsWith("0") ? "1" : "0");

        assertThatThrownBy(() -> DataKeys.current(KEY, SPACE_SALT).decrypt(altered)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void another_salt_is_another_key() {
        String stored = DataKeys.current(KEY, SPACE_SALT).encrypt("Loyer");

        assertThatThrownBy(() -> DataKeys.current(KEY, "fedcba9876543210fedcba9876543210").decrypt(stored))
            .isInstanceOf(RuntimeException.class);
    }
}
