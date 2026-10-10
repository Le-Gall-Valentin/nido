package com.nido.api.infrastructure.encryption;

import com.nido.api.shared.security.EncryptionKey;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * The keys of the data: the master key and a salt make one. The only place that derives them.
 *
 * <p>{@link #current}: Spring Security's {@code AesGcmBytesEncryptor.withPassword} — PBKDF2-HMAC-SHA256, 600 000
 * iterations, AES-256-GCM — over the salt behind a label of its own, {@value #CURRENT_LABEL}. The key fingerprint the
 * database keeps (KeyFingerprint) is PBKDF2-HMAC-SHA256 at 600 000 iterations too, over 16 random bytes: without the
 * label, a space given the fingerprint's salt by someone able to write to the database would have, for key, the
 * fingerprint stored in the instance table. With it, the salt of a data key is longer than any fingerprint salt, and the
 * two can never be the same. Deriving a key costs ~130 ms (~5 ms for the legacy key), which is why the keys of spaces
 * and users are cached — see EncryptorCache.
 *
 * <p>{@link #legacy}: what {@code Encryptors.delux} derived up to 0.15.x — PBKDF2-HMAC-SHA1, 1 024 iterations, 256 bits,
 * the same AES-256-GCM with a 16-byte IV. Spring Security deprecates delux and its AesBytesEncryptor, so the derivation
 * is done here, through the JCA, for reading only: it opens what earlier versions stored until the migration has
 * brought it to the current key. It goes when upgrades from 0.15.x stop being supported — see EncryptionBackfillRunner.
 */
public final class DataKeys {

    static final String CURRENT_LABEL = "nido/data-key:";
    private static final String CURRENT_LABEL_HEX = new String(Hex.encode(CURRENT_LABEL.getBytes(StandardCharsets.UTF_8)));
    private static final String LEGACY_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int LEGACY_ITERATIONS = 1024;
    private static final int KEY_BITS = 256;

    private DataKeys() {}

    public static TextEncryptor current(EncryptionKey key, String hexSalt) {
        return new HexTextEncryptor(AesGcmBytesEncryptor.withPassword(key.value(), CURRENT_LABEL_HEX + hexSalt).build());
    }

    public static LegacyDecryptor legacy(EncryptionKey key, String hexSalt) {
        return new LegacyDecryptor(new HexTextEncryptor(legacyBytes(key.value(), hexSalt)));
    }

    /** Package-private for the tests' LegacyKeys, the only code allowed to write with it. */
    static BytesEncryptor legacyBytes(String password, String hexSalt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Hex.decode(hexSalt), LEGACY_ITERATIONS, KEY_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance(LEGACY_ALGORITHM).generateSecret(spec).getEncoded();
            return AesGcmBytesEncryptor.withSecretKey(new SecretKeySpec(key, "AES")).build();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(LEGACY_ALGORITHM + " is not available in this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }
}
